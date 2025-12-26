package com.darkdragon.unifiedtweaks.bind;

import com.darkdragon.unifiedtweaks.UnifiedTweaks;
import com.darkdragon.unifiedtweaks.api.UTRebindableListener;
import com.darkdragon.unifiedtweaks.bot.BotManager;
import com.darkdragon.unifiedtweaks.debug.UTTrace;
import com.darkdragon.unifiedtweaks.mixin.accessor.ConnAccess;
import com.darkdragon.unifiedtweaks.mixin.accessor.ServerCommonAccess;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class HardBindManager {
    private HardBindManager() {}

    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();

    private record Session(
            UUID playerUuid,
            UUID botUuid,
            Connection networkConnection,                  // 真实 TCP Connection
            ServerGamePacketListenerImpl playerListener,    // controller 原本的 play listener
            ServerGamePacketListenerImpl botListener,  // bot 原本的 fake listener（FakeClientConnection）
            int latency,
            boolean transferred
    ) {}

    public static boolean isHardBound(ServerPlayer controller) {
        return SESSIONS.containsKey(controller.getUUID());
    }

    /** 允许从 bot 身上执行 hardunbind：通过 botUuid 反查 controller */
    private static UUID resolveController(UUID actor) {
        if (SESSIONS.containsKey(actor)) return actor;
        for (var e : SESSIONS.entrySet()) {
            if (e.getValue().botUuid.equals(actor)) return e.getKey();
        }
        return null;
    }

    public static void hardBind(ServerPlayer player, ServerPlayer bot) {
        if (isHardBound(player)) throw new IllegalStateException("Already hard-bound. Use /utbot hardunbind first.");

        // 1) 保存 controller 当前真实 listener（play）
        ServerGamePacketListenerImpl playerListener = player.connection;

        // 2) 通过父类 ServerCommonPacketListenerImpl 拿到底层真实 Connection
        //这里拿到的是控制者的原始网络连接
        ServerCommonAccess common = (ServerCommonAccess) playerListener;
        Connection playerConn = common.unifiedtweaks$getConnection();

        // 3) 保存 bot 原来的 fake listener（bot 最初用 FakeClientConnection 进服时的那个）
        ServerGamePacketListenerImpl botListener = bot.connection;

        // 4) 这条 TCP 连接对应的“客户端信息”来自 controller（因为这就是 controller 的客户端）
        ClientInformation clientInfo = player.clientInformation();

        // 可选但强烈建议：把 bot 的 options 同步成客户端的（不然有些交互/可视距离/主手等会怪）
//        bot.updateOptions(clientInfo);

        // 5) 用 controller 这条连接的网络状态 + 客户端信息，为 bot 构造一个新的 play listener
        int latency = common.unifiedtweaks$getLatency();
        boolean transferred = common.unifiedtweaks$isTransferred();
        CommonListenerCookie cookie = new CommonListenerCookie(bot.getGameProfile(), latency, clientInfo, transferred);

        // 关键：重新构造 ServerGamePacketListenerImpl
        // 构造函数内部会做：this.player = bot；bot.connection = this；
//        ServerGamePacketListenerImpl botNewPlayListener =
//                new ServerGamePacketListenerImpl(player.level().getServer(), playerConn, bot, cookie);
//
//        // 6) 把底层 Connection 的 packetListener 指向新的 play listener（这才叫“接到正确的 Connection 上”）
//        ConnAccess connAcc = (ConnAccess) playerConn;
//        connAcc.unifiedtweaks$setPacketListener(botNewPlayListener);
//        connAcc.unifiedtweaks$setDisconnectListener(null);

        // 7) 把 controller 的 connection 换成 bot 的 fake listener，让 controller 变成“服务器托管身体”
        // 这样服务器逻辑里若误用 controller.connection.send(...)，也不会再往真实客户端乱发包
//        RebindAccessors.rebind(botListener,player);
        if (botListener instanceof UTRebindableListener) {
            ((UTRebindableListener) botListener).ut$rebindTo(player);
        }

        if (playerListener instanceof UTRebindableListener) {
            ((UTRebindableListener) playerListener).ut$rebindTo(bot);
        }

        ResyncUtil.forceResync(bot);

        UTTrace.begin(player.level().getServer(), bot, 200);
        UnifiedTweaks.LOGGER.info("[TRACE] begin for bot={} uuid={}", bot.getName(), bot.getUUID());

//        botListener.player = player;
//        player.connection = botListener;

        // 8) 记录会话，用于回滚
        SESSIONS.put(player.getUUID(), new Session(
                player.getUUID(),
                bot.getUUID(),
                playerConn,
                playerListener,
                botListener,
                latency,
                transferred
        ));

        // 重要：这不是“全部问题都解决”的点，但为了让输入不被门控丢掉，你通常还需要让 bot 进入 loaded 状态
        // 你如果暂时只想验证“listener 是否真的切换成功”，可以先不动它；否则建议立即放开：
        //直接设置true会导致客户端和服务器不同步的问题，需要服务器向客户端发包让客户端重新载入数据才行
        bot.setClientLoaded(true);
    }

    public static void hardUnbind(ServerPlayer actor) {
        //传入的actor似乎是命令的执行者，就是不知道是bot还是真玩家
        UUID controllerId = resolveController(actor.getUUID());
        UnifiedTweaks.LOGGER.info("命令执行者的UUID是:"+ controllerId);
        if (controllerId == null) throw new IllegalStateException("Not hard-bound.");

        Session s = SESSIONS.remove(controllerId);
        if (s == null) throw new IllegalStateException("Not hard-bound.");

        ServerPlayer controller = actor.level().getServer().getPlayerList().getPlayer(controllerId);
        ServerPlayer bot = BotManager.getBotByUuid(s.botUuid);

        if (controller == null) {
            throw new IllegalStateException("controller 不在线，无法解绑");
        }

        // 1) 恢复 controller 的真实 play listener 到底层 Connection
        ConnAccess connAcc = (ConnAccess) s.networkConnection;
        connAcc.unifiedtweaks$setPacketListener(s.playerListener);
        connAcc.unifiedtweaks$setDisconnectListener(null);

        // 2) 恢复 controller.connection 指回原 listener
        controller.connection = s.playerListener;
        s.playerListener.player = controller;

        // 3) 恢复 bot.connection 回它自己的 fake listener（让 bot 回到“托管/挂机”状态）
        if (bot != null) {
            s.botListener.player = bot;
            bot.connection = s.botListener;
        }

        ResyncUtil.forceResync((ServerPlayer) actor.level().getPlayerByUUID(s.playerUuid()));
    }
}

final class ResyncUtil {
    private ResyncUtil() {}

    public static void forceResync(ServerPlayer p) {
        ServerLevel level = p.level();
        var server = level.getServer();

        // 0) 先关容器，避免 containerId/stateId 不一致
        p.closeContainer();

        // 1) 发“重生/重建世界信息”的关键包（客户端会重置不少本地状态）
        // 注意：不同版本构造器参数可能略有差异，以你 Mojmap 的签名为准。
        p.connection.send(new ClientboundRespawnPacket(
                p.createCommonSpawnInfo(level),
                (byte) 0 // keep same portal cooldown / flags; 0 通常够用
        ));

        // 2) 位置同步（等价于“你现在就在这里”）
//        p.connection.teleport(p.getX(), p.getY(), p.getZ(), p.getYRot(), p.getXRot());

        // 3) 基础状态同步
        var levelData = level.getLevelData();
        p.connection.send(new ClientboundChangeDifficultyPacket(levelData.getDifficulty(), levelData.isDifficultyLocked()));
        p.connection.send(new ClientboundPlayerAbilitiesPacket(p.getAbilities()));
        p.connection.send(new ClientboundSetHeldSlotPacket(p.getInventory().getSelectedSlot()));
        p.connection.send(new ClientboundSetExperiencePacket(p.experienceProgress, p.totalExperience, p.experienceLevel));

        // 4) 效果/权限/世界信息
        server.getPlayerList().sendActivePlayerEffects(p);
        server.getPlayerList().sendPlayerPermissionLevel(p);
        server.getPlayerList().sendLevelInfo(p, level);

        // 5) 背包/容器完整状态刷新（关键）
        p.containerMenu.broadcastFullState();
    }
}
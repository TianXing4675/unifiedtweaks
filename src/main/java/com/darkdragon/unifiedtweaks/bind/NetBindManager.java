package com.darkdragon.unifiedtweaks.bind;

import com.darkdragon.unifiedtweaks.bot.BotManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetCameraPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class NetBindManager {
    private NetBindManager() {}

    private static final Map<UUID, BindSession> SESSIONS = new ConcurrentHashMap<>();

    public static boolean isBound(ServerPlayer controller) {
        return SESSIONS.containsKey(controller.getUUID());
    }

    public static ServerPlayer getBoundBot(ServerPlayer controller) {
        BindSession s = SESSIONS.get(controller.getUUID());
        if (s == null) return null;
        return BotManager.getBotByUuid(s.botUuid);
    }

    public static void bind(ServerPlayer controller, ServerPlayer bot) {
        if (isBound(controller)) {
            throw new IllegalStateException("你已经在接管状态，先 /utbot unbind");
        }
        if (bot == null) throw new IllegalArgumentException("bot is null");

        // 记录原位置，用于 unbind 回去
        BindSession s = new BindSession(
                bot.getUUID(),
                controller.level().dimension(),
                controller.getX(), controller.getY(), controller.getZ(),
                controller.getYRot(), controller.getXRot()
        );
        SESSIONS.put(controller.getUUID(), s);

        // 视角切到 bot（不需要客户端 mod）
        controller.connection.send(new ClientboundSetCameraPacket(bot));
        controller.sendSystemMessage(Component.literal("已网络重绑定到假人: " + bot.getName()));
    }

    public static void unbind(ServerPlayer controller) {
        BindSession s = SESSIONS.remove(controller.getUUID());
        if (s == null) {
            throw new IllegalStateException("你当前不在接管状态");
        }

        // 视角切回自己
        controller.connection.send(new ClientboundSetCameraPacket(controller));

        // 传回原位置
        var level = controller.level().getServer().getLevel(s.originDim);
        if (level != null) {
            controller.teleportTo(level, s.originX, s.originY, s.originZ,Set.of(), s.originYaw, s.originPitch,false);
        }

        controller.sendSystemMessage(Component.literal("已解除网络重绑定"));
    }

    /**
     * 每 tick 把 controller “挂”在 bot 附近，避免 chunk/追踪/交互距离乱套。
     * 我们把 controller 放在 bot 头顶 2 格，不与 bot 碰撞。
     */
    public static void tick(MinecraftServer server) {
        for (var it = SESSIONS.entrySet().iterator(); it.hasNext(); ) {
            var e = it.next();
            UUID controllerId = e.getKey();
            BindSession s = e.getValue();

            ServerPlayer controller = server.getPlayerList().getPlayer(controllerId);
            if (controller == null) {
                it.remove();
                continue;
            }

            ServerPlayer bot = BotManager.getBotByUuid(s.botUuid);
            if (bot == null) {
                // bot 被 kill/kick 了：自动解绑
                try {
                    controller.connection.send(new ClientboundSetCameraPacket(controller));
                } catch (Throwable ignored) {}
                it.remove();
                controller.sendSystemMessage(Component.literal("假人不存在，已自动解除接管"));
                continue;
            }

            // 保持在同维度 + 同位置附近（头顶 2 格）
            if (controller.level() != bot.level()) {
                controller.teleportTo(bot.level(), bot.getX(), bot.getY() + 2.0, bot.getZ(), Set.of(), controller.getYRot(), controller.getXRot(),false);
            } else {
                controller.teleportTo(bot.level(), bot.getX(), bot.getY() + 2.0, bot.getZ(), Set.of(),controller.getYRot(), controller.getXRot(),false);
            }
        }
    }

    private record BindSession(
            UUID botUuid,
            net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> originDim,
            double originX, double originY, double originZ,
            float originYaw, float originPitch
    ) {}
}

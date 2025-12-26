package com.darkdragon.unifiedtweaks.bot;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BotManager {
    private static final Map<String, ServerPlayer> BOTS = new ConcurrentHashMap<>();

    private BotManager() {}

    public static void tick(MinecraftServer server) {
        // 后续你要加“自动右键/攻击/移动”，就在这里遍历 BOTS 做行为驱动。
        // 目前保持为空也没问题：bot 作为真正的 ServerPlayer 会被服务端常规 tick。
    }

    public static boolean isBotOnline(String name) {
        return BOTS.containsKey(name);
    }

    public static ServerPlayer getBot(String name) {
        return BOTS.get(name);
    }

    public static Map<String, ServerPlayer> getAllBots() {
        return Map.copyOf(BOTS);
    }

    public static ServerPlayer getBotByUuid(UUID uuid) {
        for (ServerPlayer p : BOTS.values()) { // BOTS 是你保存 bot 的 Map<String, ServerPlayer>
            if (p != null && p.getUUID().equals(uuid)) return p;
        }
        return null;
    }

    public static ServerPlayer getBotByName(String name) {
        return BOTS.get(name.toLowerCase(java.util.Locale.ROOT));
    }


    public static ServerPlayer spawnBot(ServerPlayer owner, String name) {
        String key = name.toLowerCase(Locale.ROOT);
        var server = owner.level().getServer();
        var level  = owner.level();

        var uuid = java.util.UUID.nameUUIDFromBytes(("UnifiedTweaks:" + name).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var profile = new com.mojang.authlib.GameProfile(uuid, name);

        var info = net.minecraft.server.level.ClientInformation.createDefault();
        var bot = new net.minecraft.server.level.ServerPlayer(server, level, profile, info);

        // placeNewPlayer 之前只做纯坐标赋值（不要 moveTo/teleportTo）
        bot.setPos(owner.getX(), owner.getY(), owner.getZ());
        bot.setYRot(owner.getYRot());
        bot.setXRot(owner.getXRot());
        bot.setYHeadRot(owner.getYHeadRot());

        var conn = new com.darkdragon.unifiedtweaks.bot.FakeClientConnection();

        // 这里按你工程当前 CommonListenerCookie 的构造/工厂方法来：
        // 方案A：如果有 createInitial/profile 工厂就优先用
        // var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(profile, false);

        // 方案B：用你截图那种 new CommonListenerCookie(profile, 0, info, false, ...)
        var cookie = new net.minecraft.server.network.CommonListenerCookie(profile, 0, info, false);

        server.getPlayerList().placeNewPlayer(conn, bot, cookie);
        BOTS.put(key, bot);
        return bot;
    }


    public static void killBot(MinecraftServer server, String name, String reason) {
        ServerPlayer bot = BOTS.remove(name);
        if (bot == null) return;

        // 先断开连接（即便是假连接，也让状态收敛）
        bot.connection.disconnect(Component.literal(reason));

        // 再从 PlayerList 移除（不同版本可能叫 remove/removeAll/removePlayer 等；这里以 remove 为主）
        // 如果你这里编译报方法名不匹配，把 remove 改成 IDE 提示的对应方法即可。
        server.getPlayerList().remove(bot);

        server.getPlayerList().broadcastSystemMessage(
                Component.literal("[UnifiedTweaks] Bot left: " + name),
                false
        );
    }

    private static UUID offlineUuid(String name) {
        // 与离线模式 UUID 规则一致
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
    }
}

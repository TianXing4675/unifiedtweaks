package com.darkdragon.unifiedtweaks.bot;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;

import java.nio.charset.StandardCharsets;
import java.util.*;
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

    private static UUID offlineUuid(String name) {
        // 与离线模式 UUID 规则一致
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
    }

    public static ServerPlayer spawnBot(MinecraftServer server, String name, ServerLevel level, BlockPos pos) {
        String key = name.toLowerCase(Locale.ROOT);

        if (BOTS.containsKey(key)) {
            throw new IllegalStateException("Bot already exists: " + name);
        }
        if (server.getPlayerList().getPlayerByName(name) != null) {
            throw new IllegalStateException("A real player with the same name is online: " + name);
        }

        UUID uuid = offlineUuid(name);
        GameProfile profile = new GameProfile(uuid, name);

        ClientInformation info = ClientInformation.createDefault();
        ServerPlayer bot = new ServerPlayer(server, level, profile, info);

        // placeNewPlayer 之前只 setPos，不要 moveTo/teleportTo
        bot.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);

        FakeClientConnection conn = new FakeClientConnection();

        CommonListenerCookie cookie = new CommonListenerCookie(profile, 0, info, false);
        server.getPlayerList().placeNewPlayer(conn, bot, cookie);

        // ✅ 关键：写入集合（放在 placeNewPlayer 之后）
        BOTS.put(key, bot);

        return bot;
    }

    public static boolean killBot(MinecraftServer server, String name, String reason) {
        String key = name.toLowerCase(Locale.ROOT);
        ServerPlayer bot = BOTS.remove(key);
        if (bot == null) return false;
        if (reason == null) reason = "No more be needed.";
        bot.connection.disconnect(Component.literal(reason));

        // 你之前说 /kick 可以踢走，说明这行路径基本没问题
        server.getPlayerList().remove(bot);
        return true;
    }

    public static List<String> listBotNames(MinecraftServer server) {
        List<String> names = new ArrayList<>();

        // 如果你想自清理残留
        BOTS.entrySet().removeIf(e -> {
            ServerPlayer p = e.getValue();
            // 玩家如果已经不在 PlayerList 里，说明被踢走/断线了
            return server.getPlayerList().getPlayer(p.getUUID()) == null;
        });

        for (ServerPlayer p : BOTS.values()) {
            names.add(String.valueOf(p.getName()));
        }
        return names;
    }

}

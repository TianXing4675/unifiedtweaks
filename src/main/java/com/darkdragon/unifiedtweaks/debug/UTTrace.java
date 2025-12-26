package com.darkdragon.unifiedtweaks.debug;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public final class UTTrace {
    private UTTrace() {}

    private static UUID focus;        // 只跟踪这个玩家
    private static int untilTick = -1; // 跟踪到哪个 tick 结束

    public static void begin(MinecraftServer server, ServerPlayer player, int ticks) {
        focus = player.getUUID();
        untilTick = server.getTickCount() + ticks;
    }

    public static boolean enabledFor(MinecraftServer server, ServerPlayer player) {
        if (focus == null) return false;
        return server.getTickCount() <= untilTick && player.getUUID().equals(focus);
    }
}

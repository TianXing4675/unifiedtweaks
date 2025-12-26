package com.darkdragon.unifiedtweaks.api;

import net.minecraft.server.level.ServerPlayer;

public interface UTRebindableListener {
    /** 把这个连接的“当前玩家”重绑定为 newPlayer（并同步 newPlayer.connection 指回本 listener） */
    void ut$rebindTo(ServerPlayer newPlayer);
}

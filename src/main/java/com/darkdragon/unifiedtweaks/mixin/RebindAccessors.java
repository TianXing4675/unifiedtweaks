package com.darkdragon.unifiedtweaks.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 同一文件内：
 * - 2 个顶级(包可见) @Mixin 接口（真正会被 mixin 系统加载）
 * - 1 个 public 工具类（业务代码调用 bind）
 */

// 真正的 mixin 1：改 ServerGamePacketListenerImpl.player
@Mixin(ServerGamePacketListenerImpl.class)
interface UT_SGPLPlayerAccessor {
    @Accessor("player")
    void ut$setPlayer(ServerPlayer player);
}

// 真正的 mixin 2：改 ServerPlayer.connection
@Mixin(ServerPlayer.class)
interface UT_ServerPlayerConnAccessor {
    @Accessor("connection")
    void ut$setConnection(ServerGamePacketListenerImpl connection);
}

// 工具类：业务代码调用这个
public final class RebindAccessors {
    private RebindAccessors() {}

    public static void bind(ServerGamePacketListenerImpl listener, ServerPlayer newPlayer) {
        ((UT_SGPLPlayerAccessor) listener).ut$setPlayer(newPlayer);
        ((UT_ServerPlayerConnAccessor) newPlayer).ut$setConnection(listener);

        // 强烈建议：否则“换人后移动校验/回弹/动不了”非常常见
        listener.resetPosition();
    }
}

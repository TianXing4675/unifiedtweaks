package com.darkdragon.unifiedtweaks.mixin.accessor;

import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerGamePacketListenerImpl.class)
public interface ServerGamePacketListenerImplAccessor {

    // ---- 关键：让 updateAwaitingTeleport() 直接短路掉移动处理 ----
    @Accessor("awaitingPositionFromClient")
    Vec3 ut$getAwaitingPositionFromClient();

    @Accessor("awaitingPositionFromClient")
    void ut$setAwaitingPositionFromClient(Vec3 v);

    @Accessor("awaitingTeleportTime")
    void ut$setAwaitingTeleportTime(int v);

    @Accessor("tickCount")
    int ut$getTickCount();

    // ---- 防止“漂浮踢出”等反作弊残留状态 ----
    @Accessor("clientIsFloating")
    void ut$setClientIsFloating(boolean v);

    @Accessor("aboveGroundTickCount")
    void ut$setAboveGroundTickCount(int v);

    @Accessor("clientVehicleIsFloating")
    void ut$setClientVehicleIsFloating(boolean v);

    @Accessor("aboveGroundVehicleTickCount")
    void ut$setAboveGroundVehicleTickCount(int v);

    // ---- 移动包频率计数，避免“过于频繁”残留 ----
    @Accessor("receivedMovePacketCount")
    int ut$getReceivedMovePacketCount();

    @Accessor("knownMovePacketCount")
    int ut$getKnownMovePacketCount();

    @Accessor("receivedMovePacketCount")
    void ut$setReceivedMovePacketCount(int v);

    @Accessor("knownMovePacketCount")
    void ut$setKnownMovePacketCount(int v);

    @Accessor("receivedMovementThisTick")
    void ut$setReceivedMovementThisTick(boolean v);
}
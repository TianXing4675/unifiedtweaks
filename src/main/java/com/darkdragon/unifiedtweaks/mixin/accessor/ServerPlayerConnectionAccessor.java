package com.darkdragon.unifiedtweaks.mixin.accessor;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerPlayer.class)
public interface ServerPlayerConnectionAccessor {
    @Accessor("connection")
    void ut$setConnection(ServerGamePacketListenerImpl connection);
}

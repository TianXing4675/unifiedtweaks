package com.darkdragon.unifiedtweaks.mixin.accessor;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.network.ServerPlayerConnection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerGamePacketListenerImpl.class)
public interface SGPLExt {
    @Accessor("player")
    void unifiedtweaks$setPlayer(ServerPlayer player);
}
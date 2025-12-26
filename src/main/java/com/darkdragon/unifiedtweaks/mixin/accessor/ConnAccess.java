package com.darkdragon.unifiedtweaks.mixin.accessor;

import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Connection.class)
public interface ConnAccess {
    @Accessor("packetListener")
    void unifiedtweaks$setPacketListener(PacketListener listener);

    @Accessor("disconnectListener")
    void unifiedtweaks$setDisconnectListener(PacketListener listener);
}

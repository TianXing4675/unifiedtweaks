package com.darkdragon.unifiedtweaks.mixin;

import com.darkdragon.unifiedtweaks.api.UTRebindableListener;
import com.darkdragon.unifiedtweaks.mixin.accessor.ServerPlayerConnectionAccessor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplRebindMixin implements UTRebindableListener {

    @Shadow public ServerPlayer player;

    @Unique
    public void ut$rebindTo(ServerPlayer newPlayer) {
        // 1) listener.player = newPlayer
        this.player = newPlayer;

        // 2) newPlayer.connection = this
        ((ServerPlayerConnectionAccessor)newPlayer)
                .ut$setConnection((ServerGamePacketListenerImpl)(Object)this);
    }
}


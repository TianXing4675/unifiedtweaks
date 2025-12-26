package com.darkdragon.unifiedtweaks.mixin.trace;

import com.darkdragon.unifiedtweaks.UnifiedTweaks;
import com.darkdragon.unifiedtweaks.debug.UTTrace;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplTraceMixin {

    @Shadow public ServerPlayer player;

    @Inject(method = "handleMovePlayer", at = @At("HEAD"))
    private void ut$traceMove(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
        if (!UTTrace.enabledFor(this.player.level().getServer(), this.player)) return;
        UnifiedTweaks.LOGGER.info("[TRACE] MOVE player={} loaded={}",
                this.player.getName(),
                this.player.hasClientLoaded()
        );
    }

    @Inject(method = "handleInteract", at = @At("HEAD"))
    private void ut$traceInteract(ServerboundInteractPacket packet, CallbackInfo ci) {
        if (!UTTrace.enabledFor(this.player.level().getServer(), this.player)) return;

        if (!(this.player.level() instanceof ServerLevel level)) return;
        Entity target = packet.getTarget(level);

        UnifiedTweaks.LOGGER.info("[TRACE] INTERACT by={} target={} targetId={} selfId={} loaded={}",
                this.player.getName(),
                target == null ? "null" : target.getType().toString(),
                target == null ? -1 : target.getId(),
                this.player.getId(),
                this.player.hasClientLoaded()
        );
    }

    @Inject(method = "handleContainerClick", at = @At("HEAD"))
    private void ut$traceContainerClick(ServerboundContainerClickPacket packet, CallbackInfo ci) {
        if (!UTTrace.enabledFor(this.player.level().getServer(), this.player)) return;

        UnifiedTweaks.LOGGER.info("[TRACE] CONTAINER_CLICK by={} menuId(server)={} menuClass={}",
                this.player.getName(),
                this.player.containerMenu.containerId,
                this.player.containerMenu.getClass().getName()
        );
    }
}

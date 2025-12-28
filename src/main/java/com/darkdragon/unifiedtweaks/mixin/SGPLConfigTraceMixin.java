package com.darkdragon.unifiedtweaks.mixin;

import com.darkdragon.unifiedtweaks.UnifiedTweaks;
import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.network.protocol.game.ServerboundConfigurationAcknowledgedPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class SGPLConfigTraceMixin extends ServerCommonPacketListenerImpl{

    // 继承父类的构造器 Stub（mixin class 必须能通过编译）
    protected SGPLConfigTraceMixin(MinecraftServer server, Connection connection, CommonListenerCookie cookie) {
        super(server, connection, cookie);
    }

    @Unique
    private Connection getConnection() {
        return this.connection;
    }

    @Shadow public ServerPlayer player;

    @Inject(method = "switchToConfig", at = @At("HEAD"))
    private void ut$traceSwitchToConfig(CallbackInfo ci) {
        UnifiedTweaks.LOGGER.info("[TRACE] switchToConfig() called: player={} uuid={} addr={}",
                safeName(), safeUuid(), safeAddr());
    }

    @Inject(method = "handleConfigurationAcknowledged", at = @At("HEAD"))
    private void ut$traceConfigAck(ServerboundConfigurationAcknowledgedPacket packet, CallbackInfo ci) {
        UnifiedTweaks.LOGGER.info("[TRACE] CONFIG_ACK received: player={} uuid={} addr={}",
                safeName(), safeUuid(), safeAddr());
    }

    // 断开原因（非常关键：你那次卡“重新配置中…”最后是怎么掉线的）
    @Inject(method = "onDisconnect", at = @At("HEAD"))
    private void ut$traceDisconnect(DisconnectionDetails disconnectionDetails, CallbackInfo ci) {
        UnifiedTweaks.LOGGER.warn("[TRACE] onDisconnect: player={} uuid={} addr={} reason={}",
                safeName(), safeUuid(), safeAddr(), disconnectionDetails.reason().getString());
    }

    @Unique
    private String safeName() {
        try { return player == null ? "null" : player.getName().toString(); }
        catch (Throwable t) { return "<?>"; }
    }

    @Unique
    private String safeUuid() {
        try { return player == null ? "null" : player.getUUID().toString(); }
        catch (Throwable t) { return "<?>"; }
    }
    @Unique
    private String safeAddr() {
        try { return String.valueOf(connection.getRemoteAddress()); }
        catch (Throwable t) { return "<?>"; }
    }
}

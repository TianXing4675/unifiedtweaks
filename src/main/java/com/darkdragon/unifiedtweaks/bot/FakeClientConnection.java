package com.darkdragon.unifiedtweaks.bot;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import org.jetbrains.annotations.Nullable;

/**
 * 服务器端“假客户端连接”：不走真实网络，用 EmbeddedChannel 承载。
 */
public final class FakeClientConnection extends Connection {
    private final EmbeddedChannel embedded;

    public FakeClientConnection() {
        super(PacketFlow.SERVERBOUND);

        this.embedded = new EmbeddedChannel();

        // 让 Connection 自己完成 channel/address 的初始化（避免后续 NPE）
        // 注意：某些版本里可以用 Connection.configureInMemoryPipeline(...) 来补齐序列化/帧处理链路。
        // 如果你这里编译期找不到 configureInMemoryPipeline，就删掉这一行也能先跑，再按报错补。
        Connection.configureInMemoryPipeline(this.embedded.pipeline(), PacketFlow.SERVERBOUND);

        this.embedded.pipeline().addLast("packet_handler", this);
        this.embedded.pipeline().fireChannelActive();
    }

    @Override
    public void send(Packet<?> packet) {
        // 丢弃所有下行包，避免内存堆积
    }

    @Override
    public void send(Packet<?> packet, @Nullable ChannelFutureListener listener) {
        // 丢弃包，但如果有人挂了 listener，就回一个“成功”future，避免上层逻辑卡住
        if (listener != null) {
            try {
                ChannelFuture f = this.embedded.newSucceededFuture();
                listener.operationComplete(f);
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void send(Packet<?> packet, @Nullable ChannelFutureListener listener, boolean flush) {
        this.send(packet, listener);
    }

    @Override
    public void disconnect(Component reason) {
        super.disconnect(reason);
        this.embedded.close();
    }
}

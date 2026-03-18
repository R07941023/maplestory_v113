package server.virtualplayer;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelId;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelProgressivePromise;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoop;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * A safe mock IOSession for virtual players that doesn't throw exceptions.
 * Used to create MapleClient instances for bots without a real network connection.
 */
public class SafeMockIOSession implements Channel {

    private static int idCounter = 0;
    private final int id;
    private volatile boolean open = true;

    public SafeMockIOSession() {
        this.id = ++idCounter;
    }

    @Override
    public ChannelId id() {
        return null;
    }

    @Override
    public EventLoop eventLoop() {
        return null;
    }

    @Override
    public Channel parent() {
        return null;
    }

    @Override
    public ChannelConfig config() {
        return null;
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public boolean isRegistered() {
        return true;
    }

    @Override
    public boolean isActive() {
        return open;
    }

    @Override
    public ChannelMetadata metadata() {
        return null;
    }

    @Override
    public SocketAddress localAddress() {
        return new InetSocketAddress("127.0.0.1", 0);
    }

    @Override
    public SocketAddress remoteAddress() {
        return new InetSocketAddress("127.0.0.1", 0);
    }

    @Override
    public ChannelFuture closeFuture() {
        return null;
    }

    @Override
    public boolean isWritable() {
        return true;
    }

    @Override
    public Unsafe unsafe() {
        return null;
    }

    @Override
    public ChannelPipeline pipeline() {
        return null;
    }

    @Override
    public ByteBufAllocator alloc() {
        return null;
    }

    @Override
    public ChannelFuture bind(SocketAddress localAddress) {
        return null;
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress) {
        return null;
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress) {
        return null;
    }

    @Override
    public ChannelFuture disconnect() {
        open = false;
        return null;
    }

    @Override
    public ChannelFuture close() {
        open = false;
        return null;
    }

    @Override
    public ChannelFuture deregister() {
        return null;
    }

    @Override
    public ChannelFuture bind(SocketAddress localAddress, ChannelPromise promise) {
        return null;
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress, ChannelPromise promise) {
        return null;
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) {
        return null;
    }

    @Override
    public ChannelFuture disconnect(ChannelPromise promise) {
        open = false;
        return null;
    }

    @Override
    public ChannelFuture close(ChannelPromise promise) {
        open = false;
        return null;
    }

    @Override
    public ChannelFuture deregister(ChannelPromise promise) {
        return null;
    }

    @Override
    public Channel read() {
        return this;
    }

    @Override
    public ChannelFuture write(Object msg) {
        // Silently discard - bots don't send to network
        return null;
    }

    @Override
    public ChannelFuture write(Object msg, ChannelPromise promise) {
        // Silently discard
        return null;
    }

    @Override
    public Channel flush() {
        return this;
    }

    @Override
    public ChannelFuture writeAndFlush(Object msg, ChannelPromise promise) {
        // Silently discard - this is the key method that must not throw
        return null;
    }

    @Override
    public ChannelFuture writeAndFlush(Object msg) {
        // Silently discard - this is the key method that must not throw
        return null;
    }

    @Override
    public ChannelPromise newPromise() {
        return null;
    }

    @Override
    public ChannelProgressivePromise newProgressivePromise() {
        return null;
    }

    @Override
    public ChannelFuture newSucceededFuture() {
        return null;
    }

    @Override
    public ChannelFuture newFailedFuture(Throwable cause) {
        return null;
    }

    @Override
    public ChannelPromise voidPromise() {
        return null;
    }

    @Override
    public <T> Attribute<T> attr(AttributeKey<T> key) {
        return null;
    }

    @Override
    public <T> boolean hasAttr(AttributeKey<T> key) {
        return false;
    }

    @Override
    public int compareTo(Channel o) {
        if (o == null) return 1;
        if (o instanceof SafeMockIOSession) {
            return Integer.compare(this.id, ((SafeMockIOSession) o).id);
        }
        return Integer.compare(System.identityHashCode(this), System.identityHashCode(o));
    }
}

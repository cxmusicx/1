package dev.splitcraft.session;

import dev.splitcraft.mixin.ConnectionAccessor;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.Packet;

/** A network connection that goes nowhere: player 2 lives inside the same JVM as the server. */
public class FakeConnection extends Connection {
    public final EmbeddedChannel fake = new EmbeddedChannel();
    public FakeConnection() {
        super(PacketFlow.SERVERBOUND);
        ((ConnectionAccessor) (Object) this).splitcraft$setChannel(fake);
    }
    public void send(Packet<?> packet) {}                       // swallow everything the server wants to send P2
    public void setReadOnly() {}
    public void handleDisconnection() {}
}

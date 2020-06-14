package UMQP.protocol;

import java.nio.ByteBuffer;

public class ConnectMessage implements ByteSerializeable {
    public final ConnectionMode mode;

    public ConnectMessage(ConnectionMode mode) {
        this.mode = mode;
    }

    @Override
    public ByteBuffer serialize() {
        return ByteBuffer.allocate(4).putInt(this.mode.mode);
    }

    public static ConnectMessage fromByteBuffer(ByteBuffer buffer) {
        return new ConnectMessage(buffer.getInt(0) == 1 ? ConnectionMode.PRODUCER : ConnectionMode.CONSUMER);
    }
}

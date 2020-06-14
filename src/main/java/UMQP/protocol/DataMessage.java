package UMQP.protocol;

import java.nio.ByteBuffer;

public class DataMessage extends ProtocolMessage {
    public final byte[] data;

    public DataMessage(byte[] data) {
        this.data = data;
    }

    @Override
    public ByteBuffer serialize() {
        return ByteBuffer.wrap(data);
    }

    public static DataMessage fromByteBuffer(ByteBuffer buffer){
        return new DataMessage(buffer.array());
    }
}

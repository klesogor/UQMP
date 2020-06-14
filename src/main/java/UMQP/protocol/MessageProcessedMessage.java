package UMQP.protocol;

import java.nio.ByteBuffer;

public class MessageProcessedMessage extends ProtocolMessage{
    @Override
    public ByteBuffer serialize() {
        return ByteBuffer.allocate(0);
    }
}

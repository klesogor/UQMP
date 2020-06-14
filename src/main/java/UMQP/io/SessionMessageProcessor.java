package UMQP.io;

import java.nio.ByteBuffer;

public interface SessionMessageProcessor extends MessageProcessor {
    void handleConnection(ByteBuffer buffer);
}

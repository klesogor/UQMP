package UMQP.io;

import java.nio.ByteBuffer;

public interface MessageProcessor {
    void processMessage(ByteBuffer buffer);
}

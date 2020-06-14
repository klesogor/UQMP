package UMQP.protocol;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.nio.ByteBuffer;

public final class SessionAck extends ProtocolMessage{

    @Override
    public ByteBuffer serialize() {
        return ByteBuffer.allocate(0);
    }
}

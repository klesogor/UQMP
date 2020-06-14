package UMQP.io;

import java.net.SocketAddress;
import java.nio.ByteBuffer;

public interface ConnectionHandler {
    void onConnect(SocketAddress address, ByteBuffer connectionData);
}

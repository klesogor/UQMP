package UMQP.io;

import UMQP.protocol.SessionAck;
import UMQP.protocol.TransportMessageTypes;
import UMQP.utils.BufferHelper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.TimerTask;
import java.util.function.Function;

public class ServerSession implements MessageProcessor {
    public final Connection connection;
    private MessageProcessor messageHandler;
    private Function<Integer, Void> disconnectHandler;
    private boolean keptAlive = false;

    public ServerSession(MessageProcessor messageHandler, SocketAddress address, Multiplexer io, Integer sessionId) throws IOException {
        this.messageHandler = messageHandler;
        this.connection = new Connection(sessionId, io, address);
        this.connection.sendMessageNoAck(new SessionAck(), TransportMessageTypes.SESSION_CONFIRMED);
        this.connection.scheduler.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (!keptAlive) {
                    disconnectHandler.apply(connection.sessionId);
                }

                keptAlive = false;
            }
        }, connection.TIMEOUT, connection.TIMEOUT);

        connection.registerNextHandler(this);
    }

    public ServerSession onDisconnect(Function<Integer, Void> handler) {
        this.disconnectHandler = handler;
        return this;
    }

    @Override
    public void processMessage(ByteBuffer buffer) {
        if(BufferHelper.messageType(buffer) == TransportMessageTypes.ECHO.value){
            this.keptAlive = true;
            return;
        }

        this.messageHandler.processMessage(buffer);
    }
}

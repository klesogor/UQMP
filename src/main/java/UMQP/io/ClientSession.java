package UMQP.io;

import UMQP.protocol.ConnectMessage;
import UMQP.protocol.TransportMessageTypes;
import UMQP.utils.BufferHelper;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

public final class ClientSession implements MessageProcessor {
    public final Connection connection;
    private ConnectionState state = ConnectionState.NOT_CONNECTED;
    private SessionMessageProcessor nextHandler;
    private CompletableFuture<Void> sessionFeature;

    public ClientSession(Connection connection) {
        this.connection = connection;
        this.connection.registerNextHandler(this);
    }

    public CompletableFuture<Void> connect(ConnectMessage connectionDetails) throws IOException {
        this.state = ConnectionState.PENDING;
        this.connection.scheduler.schedule(this.checkConnected(), 2000);
        this.sessionFeature = new CompletableFuture<>();
        this.connection.sendMessageNoAck(connectionDetails, TransportMessageTypes.SESSION_OPEN);

        return this.sessionFeature;
    }

    public ClientSession registerNextHandler(SessionMessageProcessor processor){
        this.nextHandler = processor;
        return this;
    }

    @Override
    public void processMessage(ByteBuffer buffer) {
        var messageType = BufferHelper.messageType(buffer);
        if (this.state != ConnectionState.CONNECTED && messageType != TransportMessageTypes.SESSION_CONFIRMED.value) {
            return;
        }
        if (this.state == ConnectionState.PENDING) {
            if(this.nextHandler != null) {
                this.nextHandler.handleConnection(BufferHelper.messageBody(buffer));
            }
            this.sessionFeature.complete(null);
            this.connection.scheduler.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    try {
                        connection.sendMessageNoAck(() -> ByteBuffer.allocate(0), TransportMessageTypes.ECHO);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }, this.connection.TIMEOUT / 3, this.connection.TIMEOUT / 3);
            this.state = ConnectionState.CONNECTED;

            return;
        }

        this.nextHandler.processMessage(buffer);
    }

    private TimerTask checkConnected(){
        return new TimerTask() {
            @Override
            public void run() {
                if(state == ConnectionState.CONNECTED){
                    return;
                }

                sessionFeature.completeExceptionally(new TimeoutException("E_UMQP_SESSION_TIMEOT"));
            }
        };
    }

    public enum ConnectionState {
        CONNECTED,
        PENDING,
        NOT_CONNECTED
    }
}

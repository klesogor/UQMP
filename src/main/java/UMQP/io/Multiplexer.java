package UMQP.io;

import UMQP.protocol.TransportMessage;
import UMQP.protocol.TransportMessageTypes;
import UMQP.utils.BufferHelper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Multiplexer {


    private final DatagramChannel chan;
    private final ByteBuffer readBuffer;
    private final Map<Integer, MessageProcessor> handlers = new ConcurrentHashMap<>();
    public final int listenPort;
    private ConnectionHandler fallbackProcessor;

    public Multiplexer setFallbackProcessor(ConnectionHandler fallbackProcessor) {
        this.fallbackProcessor = fallbackProcessor;
        return this;
    }

    private volatile boolean isRunning = false;

    public Multiplexer(Integer bufferSize, int listenPort) throws IOException {
        this.listenPort = listenPort;
        this.chan = DatagramChannel.open();
        readBuffer = ByteBuffer.allocate(bufferSize);
        chan.socket().bind(new InetSocketAddress(listenPort));
    }

    public void sendMessage(TransportMessage message, SocketAddress address, TransportMessageTypes type) throws IOException {
        this.chan.send(message.toBuffer(type.value).rewind(), address);
    }

    public void acknowledge(Integer seq, Integer sessionId, SocketAddress address) throws IOException {
        this.chan.send(ByteBuffer.allocate(1 + 4 + 4).put(TransportMessageTypes.PACKET_RECEIVED.value).putInt(seq).putInt(sessionId).rewind(), address);
    }

    public void registerHandler(Integer sessionId, MessageProcessor handler) {
        this.handlers.put(sessionId, handler);
    }

    public void listen() throws IOException {
        this.isRunning = true;
        System.out.printf("Listening for UDP messages on port %d\n", this.listenPort);
        while (this.isRunning) {
            this.readBuffer.clear();
            var incomingMessageAddress = this.chan.receive(this.readBuffer);
            var sessionId = BufferHelper.messageSessionId(this.readBuffer);
            if (handlers.containsKey(sessionId)) {
                this.handlers.get(sessionId).processMessage(toMessageBuffer(this.readBuffer.duplicate()));
                continue;
            }
            if (this.fallbackProcessor != null) {
                this.fallbackProcessor.onConnect(incomingMessageAddress, toMessageBuffer(this.readBuffer.duplicate()));
            }
        }
    }

    private static ByteBuffer toMessageBuffer(ByteBuffer ioBuffer) {
        var actualSize = ioBuffer.position();

        return ByteBuffer.wrap(Arrays.copyOfRange(ioBuffer.array(), 0, actualSize));
    }

    public void stop() {
        this.isRunning = false;
    }
}

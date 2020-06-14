package UMQP.consumer;

import UMQP.io.ClientSession;
import UMQP.io.Connection;
import UMQP.io.Multiplexer;
import UMQP.io.SessionMessageProcessor;
import UMQP.protocol.ConnectMessage;
import UMQP.protocol.ConnectionMode;
import UMQP.protocol.MessageProcessedMessage;
import UMQP.protocol.TransportMessageTypes;
import UMQP.serialization.MessageDeserializer;
import UMQP.utils.BufferHelper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class Consumer<T> implements SessionMessageProcessor {
    private final ClientSession connection;
    private final ThreadPoolExecutor pool = new ThreadPoolExecutor(0, 2, 0L, TimeUnit.SECONDS, new SynchronousQueue<>());
    private final MessageDeserializer<T> deserializer;
    private final MessageConsumer<T> handler;

    private Consumer(MessageConsumer<T> handler, MessageDeserializer<T> deserializer, Multiplexer multiplexer, InetSocketAddress address) {
        var random = new Random();
        this.connection = new ClientSession(new Connection(random.nextInt(Integer.MAX_VALUE), multiplexer, address));
        this.connection.registerNextHandler(this);
        this.deserializer = deserializer;
        this.handler = handler;
    }

    public static <T> Consumer<T> listen(MessageConsumer<T> handler, MessageDeserializer<T> deserializer, Multiplexer multiplexer, InetSocketAddress address) {
        var consumer = new Consumer<>(handler, deserializer, multiplexer, address);
        try {
            consumer.connection.connect(new ConnectMessage(ConnectionMode.CONSUMER));
            return consumer;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public void handleConnection(ByteBuffer buffer) {
        return;// NOT IMPLEMENTED
    }

    @Override
    public void processMessage(ByteBuffer buffer) {
        this.pool.execute(() -> {
            var data = deserializer.deserialize(BufferHelper.messageBody(buffer).array());
            this.handler.consumeMessage(data, () -> {
                this.connection.connection.sendMessage(new MessageProcessedMessage(), TransportMessageTypes.MESSAGE_PROCESSED);
            });
        });
    }
}

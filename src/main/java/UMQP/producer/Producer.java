package UMQP.producer;

import UMQP.io.ClientSession;
import UMQP.io.Connection;
import UMQP.io.Multiplexer;
import UMQP.protocol.ConnectMessage;
import UMQP.protocol.ConnectionMode;
import UMQP.protocol.DataMessage;
import UMQP.protocol.TransportMessageTypes;
import UMQP.serialization.MessageSerializer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

public final class Producer<T> {
    private final ClientSession session;
    private final MessageSerializer<T> serializer;

    private Producer(MessageSerializer<T> serializer, Multiplexer multiplexer, InetSocketAddress address) throws IOException {
        var random = new Random();
        this.session = new ClientSession(new Connection(random.nextInt(Integer.MAX_VALUE), multiplexer, address));
        this.serializer = serializer;
    }

    public static <T> CompletableFuture<Producer<T>> openChannel(MessageSerializer<T> serializer, Multiplexer multiplexer, InetSocketAddress address) throws IOException {
        var producer = new Producer<>(serializer, multiplexer, address);
        return producer.session.connect(new ConnectMessage(ConnectionMode.PRODUCER)).thenApply((absurd) -> producer);
    }

    public CompletableFuture<Void> send(T data) {
        return this.session.connection.sendMessage(new DataMessage(this.serializer.serialize(data)), TransportMessageTypes.DATA_MESSAGE);
    }
}

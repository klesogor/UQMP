package UMQP.broker;

import UMQP.broker.queue.Partition;
import UMQP.broker.queue.Queue;
import UMQP.broker.queue.QueueManager;
import UMQP.io.ConnectionHandler;
import UMQP.io.MessageProcessor;
import UMQP.io.Multiplexer;
import UMQP.io.ServerSession;
import UMQP.protocol.*;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.logging.Logger;

public final class Broker implements ConnectionHandler {
    private Multiplexer multiplexer;
    private Logger logger = Logger.getLogger("Broker");

    private Optional<ServerSession> producer = Optional.empty();
    private Optional<ServerSession> consumer = Optional.empty();

    private QueueManager queueManager = new QueueManager(new Queue("test", 1));

    public Broker(Multiplexer multiplexer) {
        this.multiplexer = multiplexer;
        this.multiplexer.setFallbackProcessor(this);
    }

    @Override
    public void onConnect(SocketAddress address, ByteBuffer connectionData) {
        try {
            var message = TransportMessage.fromBuffer(connectionData, ConnectMessage::fromByteBuffer);
            if (message.data.mode == ConnectionMode.CONSUMER) {
                this.logger.info("Connected consumer");
                this.connectConsumer(address, message);
            } else {
                this.logger.info("Connected producer");
                this.connectProducer(address, message);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void connectConsumer(SocketAddress address, TransportMessage<ConnectMessage> connectionDetails) throws IOException {
        if (this.consumer.isPresent()) {
            throw new RuntimeException("Consumer already connected");
        }

        this.consumer = Optional.of(spawnConnection(this::onConsumerMessage, address, connectionDetails).onDisconnect(id -> {
            this.consumer = Optional.empty();
            this.logger.info("Disconnected consumer");
            return null;
        }));

        this.tryPollQueue();
    }

    private void connectProducer(SocketAddress address, TransportMessage<ConnectMessage> connectionDetails) throws IOException {
        if (this.producer.isPresent()) {
            throw new RuntimeException("Producer already connected");
        }

        this.producer = Optional.of(spawnConnection(this::onProducerMessage, address, connectionDetails).onDisconnect(id -> {
            this.producer = Optional.empty();
            this.logger.info("Disconnected producer");
            return null;
        }));
    }

    private void onProducerMessage(ByteBuffer producerData) {
        var message = TransportMessage.fromBuffer(producerData, DataMessage::fromByteBuffer);
        this.queueManager.tryGetPartition("test", 1).ifPresent(partition -> partition.enqueue(message.data));

        this.tryPollQueue();
    }

    private void onConsumerMessage(ByteBuffer producerData) {
        this.queueManager.tryGetPartition("test", 1).ifPresent(Partition::notifyProcessed);
        this.tryPollQueue();
    }

    private ServerSession spawnConnection(MessageProcessor processor, SocketAddress address, TransportMessage<ConnectMessage> details) throws IOException {
        return new ServerSession(processor, address, this.multiplexer, details.sessionId);
    }

    private void tryPollQueue() {
        if (this.consumer.isEmpty()) {
            return;
        }

        this.queueManager.tryGetPartition("test", 1).flatMap(Partition::tryPoll).ifPresent(message -> {
            this.consumer.get().connection.sendMessage(message, TransportMessageTypes.DATA_MESSAGE);
        });
    }


}

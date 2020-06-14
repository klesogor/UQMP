package UMQP.io;

import UMQP.protocol.ByteSerializeable;
import UMQP.protocol.TransportMessage;
import UMQP.protocol.TransportMessageTypes;
import UMQP.utils.BufferHelper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents duplex connection between two peers
 */
public class Connection implements MessageProcessor {
    final int MAX_RETRIES = 2;
    final int RTT = 1000;
    final int TIMEOUT = 20000;

    public final Integer sessionId;

    final AtomicInteger seqSend = new AtomicInteger(1);
    final AtomicInteger seqRec = new AtomicInteger(1);
    final Timer scheduler = new Timer("Scheduler for duplex p2p connection");

    private final Multiplexer io;
    private final Map<Integer, PendingMessage> awaiterQueue = new ConcurrentHashMap<>();
    private MessageProcessor nextHandler;
    private final PriorityQueue<IncomingMessage> incomingQueue = new PriorityQueue<>(10, (t1, t2) -> {
        if (t1.seq.equals(t2.seq)) {
            return 0;
        }

        return t1.seq < t2.seq ? 1 : -1;
    });
    private SocketAddress address;

    public Connection(Integer sessionId, Multiplexer io, SocketAddress address) {
        this.sessionId = sessionId;
        this.io = io;
        this.address = address;

        this.io.registerHandler(sessionId, this);
    }

    public CompletableFuture<Void> sendMessage(ByteSerializeable message, TransportMessageTypes type) {
        var transportMessage = new TransportMessage<>(this.seqSend.getAndIncrement(), this.sessionId, message);
        var awaiter = new CompletableFuture<Void>();
        var pendingMessage = new PendingMessage(transportMessage, 0, awaiter, type);
        this.sendAcked(pendingMessage);

        return awaiter;
    }

    public void sendMessageNoAck(ByteSerializeable message, TransportMessageTypes type) throws IOException {
        this.io.sendMessage(new TransportMessage<>(0, this.sessionId, message), this.address, type);
    }

    public Connection registerNextHandler(MessageProcessor handler) {
        this.nextHandler = handler;
        return this;
    }

    @Override
    public void processMessage(ByteBuffer buffer) {
        if (BufferHelper.messageType(buffer) == TransportMessageTypes.PACKET_RECEIVED.value) {
            this.handleConfirmation(buffer);
            return;
        }

        if(TransportMessage.shouldBeAcked(buffer)){
            try {
                this.io.acknowledge(BufferHelper.messageSeq(buffer), this.sessionId, this.address);
            } catch (Exception ex){
                ex.printStackTrace();
                return;
            }
        }

        this.handleIncoming(buffer);
    }

    private void sendAcked(PendingMessage message) {
        if (message.retries > MAX_RETRIES) {
            message.awaiter.completeExceptionally(new TimeoutException("UMQP_E_TIMEOUT"));
            return;
        }
        try {
            this.io.sendMessage(message.message, this.address, message.type);
            this.awaiterQueue.put(message.message.seq, message.nextRetry());
            this.scheduler.schedule(this.assertDelivered(message.message.seq), (message.retries + 1) * 2 * this.RTT);
        } catch (IOException ex) {
            message.awaiter.completeExceptionally(ex);
        }
    }

    private void handleConfirmation(ByteBuffer confirmation){
        var seq = BufferHelper.messageSeq(confirmation);
        Optional.of(awaiterQueue.get(seq)).ifPresent(pending -> {
            this.awaiterQueue.remove(seq);
            pending.awaiter.complete(null);
        });

    }

    private void handleIncoming(ByteBuffer message) {
        // if message is not in order - dump it to message queue
        if (this.seqRec.get() < BufferHelper.messageSeq(message)) {
            this.incomingQueue.add(new IncomingMessage(BufferHelper.messageSeq(message), message));
            return;
        }
        //process received message and increment seq
        try {
            this.nextHandler.processMessage(message);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        this.seqRec.incrementAndGet();
        //while message queue is not empty and order is maintained - process messages
        while (!this.incomingQueue.isEmpty() && this.incomingQueue.peek().seq.equals(this.seqRec.get())) {
            try {
                this.processMessage(message);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            this.seqRec.incrementAndGet();
        }
    }

    private TimerTask assertDelivered(Integer seq) {
        return new TimerTask() {
            @Override
            public void run() {
                if (!awaiterQueue.containsKey(seq)) {
                    return;
                }
                var pending = awaiterQueue.get(seq);
                sendAcked(pending);
            }
        };
    }

    private static final class IncomingMessage {
        final Integer seq;
        final ByteBuffer data;

        public IncomingMessage(Integer seq, ByteBuffer data) {
            this.seq = seq;
            this.data = data;
        }
    }
}

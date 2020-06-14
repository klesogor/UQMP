package UMQP.io;

import UMQP.protocol.ByteSerializeable;
import UMQP.protocol.TransportMessage;
import UMQP.protocol.TransportMessageTypes;

import java.util.concurrent.CompletableFuture;

final class PendingMessage<T extends ByteSerializeable> {
    public final TransportMessage<T> message;
    public final Integer retries;
    public final CompletableFuture<Void> awaiter;
    public final TransportMessageTypes type;

    public PendingMessage(TransportMessage<T> message, Integer retries, CompletableFuture<Void> awaiter, TransportMessageTypes type) {
        this.message = message;
        this.retries = retries;
        this.awaiter = awaiter;
        this.type = type;
    }

    public PendingMessage<T> nextRetry(){
        return new PendingMessage<>(this.message, this.retries + 1, this.awaiter, this.type);
    }
}

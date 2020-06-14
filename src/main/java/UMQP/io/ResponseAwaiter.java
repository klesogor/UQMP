package UMQP.io;

import java.util.concurrent.CompletableFuture;

class ResponseAwaiter {
    public final PendingMessage message;
    public final CompletableFuture<Void> awaiter;

    public ResponseAwaiter(PendingMessage message, CompletableFuture<Void> awaiter) {
        this.message = message;
        this.awaiter = awaiter;
    }
}

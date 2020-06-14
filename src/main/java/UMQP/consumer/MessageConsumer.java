package UMQP.consumer;

import java.util.function.Function;

public interface MessageConsumer<T> {
    void consumeMessage(T message, Acknowledger ack);
}

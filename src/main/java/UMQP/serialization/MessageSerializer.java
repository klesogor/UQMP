package UMQP.serialization;

public interface MessageSerializer<T> {
    byte[] serialize(T object);
}

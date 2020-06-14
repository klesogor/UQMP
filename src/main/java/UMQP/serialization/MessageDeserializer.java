package UMQP.serialization;

public interface MessageDeserializer<T> {
    T deserialize(byte[] data);
}

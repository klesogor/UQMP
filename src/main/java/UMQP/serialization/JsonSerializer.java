package UMQP.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonSerializer<T> implements MessageSerializer<T>, MessageDeserializer<T> {
    private final ObjectMapper mapper = new ObjectMapper();
    private final Class<T> clazz;

    public JsonSerializer(Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public byte[] serialize(Object object) {
        try {
            return this.mapper.writeValueAsBytes(object);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return new byte[0];
    }

    @Override
    public T deserialize(byte[] data) {
        try { ;
            return this.mapper.readValue(data, this.clazz);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }
}

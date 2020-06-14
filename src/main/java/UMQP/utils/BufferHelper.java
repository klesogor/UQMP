package UMQP.utils;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class BufferHelper {
    public static ByteBuffer sliceBuffer(int offset, ByteBuffer buffer) {
        var array = buffer.duplicate().array();
        return ByteBuffer.wrap(Arrays.copyOfRange(array, offset, buffer.limit()));
    }

    public static ByteBuffer messageBody(ByteBuffer message) {
        return sliceBuffer(9, message);
    }

    public static int messageSeq(ByteBuffer message) {
        return message.getInt(1);
    }

    public static int messageSessionId(ByteBuffer message) {
        return message.getInt(5);
    }

    public static byte messageType(ByteBuffer message){
        return message.get(0);
    }
}

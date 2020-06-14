package UMQP.protocol;

import UMQP.utils.BufferHelper;

import java.nio.ByteBuffer;
import java.util.function.Function;

public final class TransportMessage<T extends ByteSerializeable>{
    public final Integer seq;
    public final Integer sessionId;
    public final T data;

    public TransportMessage(Integer seq, Integer sessionId, T data) {
        this.seq = seq;
        this.sessionId = sessionId;
        this.data = data;
    }

    public ByteBuffer toBuffer(byte transportCode){
        var dataBytes = data.serialize();
        return ByteBuffer.allocate(1 + 4 + 4 + dataBytes.limit()).put(transportCode).putInt(seq).putInt(sessionId).put(dataBytes.array());
    }

    //|1 - type | 4 - seq | 4 - session | data
    public static <T extends ByteSerializeable> TransportMessage<T> fromBuffer(ByteBuffer buffer, Function<ByteBuffer, T> deserializer){
        var data = BufferHelper.sliceBuffer(9, buffer);

        return new TransportMessage<>(buffer.getInt(1), buffer.getInt(5), deserializer.apply(data));
    }

    public static boolean shouldBeAcked(ByteBuffer transportMessageBuffer){
        return BufferHelper.messageSeq(transportMessageBuffer) != 0;
    }
}

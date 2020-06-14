package UMQP.protocol;

public enum TransportMessageTypes {
    SESSION_OPEN((byte)1),
    SESSION_CONFIRMED((byte)2),
    DATA_MESSAGE((byte)3),
    PACKET_RECEIVED((byte)4),
    ECHO((byte)5),
    MESSAGE_PROCESSED((byte)6);

    public final byte value;
    TransportMessageTypes(byte value){
        this.value = value;
    }
}

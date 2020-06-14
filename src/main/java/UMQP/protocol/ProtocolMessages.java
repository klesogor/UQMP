package UMQP.protocol;

public enum ProtocolMessages {
    CREATE_SESSION((byte)1),
    CONFIRM_SESSION((byte)2),
    DATA_PACKET((byte)3),
    ACK_PROCESSED((byte)4);


    public final byte code;
    ProtocolMessages(byte code){
        this.code = code;
    }
}

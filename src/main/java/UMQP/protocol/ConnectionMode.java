package UMQP.protocol;

public enum ConnectionMode {
    PRODUCER(1),
    CONSUMER(2);


    public final int mode;
    ConnectionMode(int mode){
        this.mode = mode;
    }
}

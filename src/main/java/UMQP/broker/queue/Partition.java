package UMQP.broker.queue;

import UMQP.protocol.DataMessage;

import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Partition {
    private final Queue<DataMessage> messageQueue = new LinkedList<>();
    private PartitionStatus status = PartitionStatus.OPEN;
    private final Lock lock = new ReentrantLock();

    public Optional<DataMessage> tryPoll(){
        lock.lock();

        if(this.status != PartitionStatus.OPEN){
            return Optional.empty();
        }
        var message = Optional.ofNullable(this.messageQueue.poll());
        if(message.isPresent()){
            this.status = PartitionStatus.AWAITING;
        }
        lock.unlock();

        return message;
    }

    public void notifyProcessed(){
        lock.lock();
        this.status = PartitionStatus.OPEN;
        lock.unlock();
    }

    public void enqueue(DataMessage message){
        this.messageQueue.add(message);
    }

    public enum  PartitionStatus{
        OPEN,
        AWAITING
    }
}

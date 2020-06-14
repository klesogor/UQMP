package UMQP.broker.queue;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class QueueManager {
    public final Map<String, Queue> queues = new ConcurrentHashMap<>();

    public QueueManager(Queue... queues) {
        for (var queue : queues) {
            this.queues.put(queue.name, queue);
        }
    }

    public Optional<Partition> tryGetPartition(String queueName, int partition) {
        return Optional.ofNullable(this.queues.get(queueName)).flatMap(queue -> queue.tryGetPartition(partition));
    }
}

package UMQP.broker.queue;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Optional;

public class Queue {
    public final String name;
    public final Map<Integer, Partition> partitions = new ConcurrentHashMap<>();

    public Queue(String name, int partitionsCount) {
        this.name = name;
        //LOL LOOP. I miss haskell so much... State + TVar WTF
        for(var i = 1; i <= partitionsCount; i++){
            partitions.put(i, new Partition());
        }
    }

    public Optional<Partition> tryGetPartition(Integer partition){
        return  Optional.ofNullable(partitions.get(partition));
    }
}

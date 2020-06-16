/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package UMQP;

import UMQP.broker.Broker;
import UMQP.consumer.Consumer;
import UMQP.io.Multiplexer;
import UMQP.producer.Producer;
import UMQP.serialization.JsonSerializer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;

public class Main {

    public static class MessageText {
        public String text;

        public MessageText() {
        }

        public MessageText(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }

    public static class BrokerDemo {

        public void run() throws IOException {
            System.out.println("Starting broker at port 5400");
            var mult = new Multiplexer(2048, 5400);
            var broker = new Broker(mult);
            mult.listen();
        }
    }

    public static class ProducerDemo {
        private Producer<MessageText> prod;

        public void run() throws IOException, ExecutionException, InterruptedException {
            System.out.println("Starting producer at port 5401");
            var mult = new Multiplexer(2048, 5401);
            var serverThread = new Thread(() -> {
                try {
                    mult.listen();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
            serverThread.start();
            var producer = Producer.openChannel(new JsonSerializer<>(MessageText.class), mult, new InetSocketAddress("localhost", 5400)).get();
            System.out.println("Connected to broker");
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

            while (true) {
                System.out.printf("Enter message to be sent: \n");
                var message = reader.readLine();
                System.out.printf("Sending text: %s \n", message);
                producer.send(new MessageText(message)).get();
            }
        }
    }

    public static class ConsumerDemo {
        public void run() throws IOException {
            var mult = new Multiplexer(2048, 5402);
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("Starting consumer at port 5402");
            var consumerInitThread = new Thread(() -> {
                try {
                    Consumer.listen((message, ack) -> {
                        System.out.printf("Received message:\n%s\nPress enter to acknowledge", message.text);
                        try {
                            reader.readLine();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        ack.acknowledge();
                    }, new JsonSerializer<>(MessageText.class), mult, new InetSocketAddress("localhost", 5400)).get();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
            consumerInitThread.start();
            mult.listen();
        }
    }

    public static void main(String[] args) {
        System.out.println("Starting demo");
        var mode = args.length > 0 ? args[0] : "";
        try {

            switch (mode.toLowerCase()) {
                case "producer":
                    var prod = new ProducerDemo();
                    prod.run();
                    break;
                case "consumer":
                    var cons = new ConsumerDemo();
                    cons.run();
                    break;
                default:
                    var broker = new BrokerDemo();
                    broker.run();
                    break;
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }
}

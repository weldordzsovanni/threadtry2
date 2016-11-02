package threadtest;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by NA on 2016.11.02..
 */
public class FeedStarter {
    private final ExecutorService feedExecutor = Executors.newFixedThreadPool(1);

    private final BlockingQueue<RequestHolder> hazelcastInQueue = new ArrayBlockingQueue<>(3);

    public static void main(String[] args) {
        new FeedStarter().start();
    }

    public void start() {
        try {
            for (int i = 0; i < Integer.MAX_VALUE; i++) {
                System.out.println("start");

                final RequestHolder nextRequest = MyHelper.getNextFromDatabase();
                hazelcastInQueue.put(nextRequest);

                System.out.println("end" + i);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            feedExecutor.shutdown();
        }
    }
}

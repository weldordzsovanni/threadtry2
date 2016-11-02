package threadtest;

import java.util.Random;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * Created by ZGY on 2016.10.29..
 */
public class CalculatorStarter {

    private static final int MAX_CALCULATION = 10000;
    private static final int TIMEOUT = 2000;
    private static final int WORKER_THREAD = 2;
//    private final ExecutorService poolCounter = Executors.newFixedThreadPool(WORKER_THREAD);

//    private final ExecutorService poolCounter =  new ThreadPoolExecutor(WORKER_THREAD, WORKER_THREAD,
//                                  0L, TimeUnit.MILLISECONDS,
//                                  new SynchronousQueue<>());//rejectec exception amikor vegzett az egyik thread: unning, pool size = 2, active threads = 1, queued tasks = 0, completed tasks = 1]

    private final ExecutorService poolCounter = new ThreadPoolExecutor(WORKER_THREAD, WORKER_THREAD,
            0L, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(WORKER_THREAD));
    private final ExecutorService poolMonitor = Executors.newFixedThreadPool(WORKER_THREAD);
    private final BlockingQueue<RequestHolder> innerBlockingQueue = new ArrayBlockingQueue<>(2);


    public static void main(String[] args) throws ExecutionException, InterruptedException, TimeoutException {
        new CalculatorStarter().start();
    }


    public void start() {
        try {
            for (int i = 0; i < Integer.MAX_VALUE; i++) {
                System.out.println("start");

                final RequestHolder nextRequest = CalcHelper.getNextFromHazelcastETLqueue();
                innerBlockingQueue.put(nextRequest);
                startCalculating(nextRequest);

                System.out.println("end" + i);
            }
        } catch (Exception ex) {
            poolCounter.shutdown();
            poolMonitor.shutdown();
            ex.printStackTrace();
        }
    }


    private void startCalculating(final RequestHolder nextRequest) {
        final CompletableFuture<ResponseHolder> calculatorFuture = CompletableFuture.supplyAsync(new Supplier<ResponseHolder>() {
            @Override
            public ResponseHolder get() {
                logger("cpuHeavyCalculation start:" + nextRequest);
                final long calculation = CalcHelper.cpuHeavyCalculation(new Random().nextInt(MAX_CALCULATION));

                if (new Random().nextInt(100) > 50) {
                    logger("exception throwed");
                    throw new RuntimeException("throw runtime: " + nextRequest);
                }
                return new ResponseHolder(nextRequest, "done: " + calculation);
            }
        }, poolCounter)
                .whenComplete(new BiConsumer<ResponseHolder, Throwable>() {
                    @Override
                    public void accept(ResponseHolder responseHolder, Throwable throwable) {
                        if (responseHolder != null) {
                            logger("SUCCESSFUL response: " + responseHolder);
                        } else {
                            logger("FAILED, exception occured: " + throwable.getMessage());
                        }
                        innerBlockingQueue.remove(nextRequest);
                    }
                });

        startMonitoring(calculatorFuture, nextRequest);

    }

    private void startMonitoring(final CompletableFuture calculatorFuture, final RequestHolder nextRequest) {
        CompletableFuture.runAsync(new Runnable() {
            @Override
            public void run() {
                try {
                    logger("monitoring start " + nextRequest);
                    calculatorFuture.get(TIMEOUT, TimeUnit.MILLISECONDS);
                    logger("monitoring done " + nextRequest);
                } catch (TimeoutException e) {
                    logger("TIMEOUT occured " + nextRequest);
                    if (nextRequest.getRetryCount() > 2) {
                        //put this element to haselcast ERROR queue
                        //no chance to stop running thread, like calculatorFuture.cancel()
                    } else {
                        nextRequest.incrementRetry();
                        //put this element BACK to haselcast queue
                    }
                    //not needed here: calculatorFuture.get();    // innerBlockingQueue.remove(nextRequest);
                } catch (InterruptedException | ExecutionException e) {
//                    e.printStackTrace();// it does not matter, only timeout is interesting
                }
            }
        }, poolMonitor);
    }

    private void logger(String log) {
        System.out.println("Thread " + Thread.currentThread().getId() + " " + log);
    }

}

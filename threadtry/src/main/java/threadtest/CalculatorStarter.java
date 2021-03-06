package threadtest;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * Created by NA on 2016.10.29..
 */
public class CalculatorStarter {

    private static final int MAX_CALCULATION = 10000;
    private static final int TIMEOUT = 2000;
    private static final int WORKER_THREAD = 2;
//    private final ExecutorService poolCounter = Executors.newFixedThreadPool(WORKER_THREAD);
//    private final ExecutorService poolCounter = new ThreadPoolExecutor(WORKER_THREAD, WORKER_THREAD, 0L, TimeUnit.MILLISECONDS, new SynchronousQueue<>());//rejectec exception amikor vegzett az egyik thread: unning, pool size = 2, active threads = 1, queued tasks = 0, completed tasks = 1]

    private final ExecutorService poolCounter = new ThreadPoolExecutor(WORKER_THREAD, WORKER_THREAD,
            0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(WORKER_THREAD));
    private final ExecutorService poolMonitor = Executors.newFixedThreadPool(WORKER_THREAD);

    private final Queue<RequestHolder> retryHazelcastQueue = new LinkedList<>();
    private final Queue<RequestHolder> errorHazelcastQueue = new LinkedList<>();
    private final Semaphore innerSemaphore = new Semaphore(WORKER_THREAD);

    public static void main(String[] args) throws ExecutionException, InterruptedException, TimeoutException {
        new CalculatorStarter().start();
    }


    public void start() {
        try {
            for (int i = 0; i < 10; i++) {
                System.out.println("start "+i);

                final RequestHolder nextRequest = determineNextRequest();
                innerSemaphore.acquire();
                startCalculating(nextRequest);

                System.out.println("end " + i);
            }
            poolCounter.shutdown();
            poolMonitor.shutdown();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void startCalculating(final RequestHolder nextRequest) {
        final CompletableFuture<ResponseHolder> calculatorFuture = CompletableFuture.supplyAsync(new Supplier<ResponseHolder>() {
            @Override
            public ResponseHolder get() {
                logger("cpuHeavyCalculation start:" + nextRequest);
                final long calculation = MyHelper.cpuHeavyCalculation(new Random().nextInt(MAX_CALCULATION));

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
                        innerSemaphore.release();
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
                        errorHazelcastQueue.offer(new RequestHolder(nextRequest.getPayload()));
                        //no chance to stop running thread, like calculatorFuture.cancel()
                    } else {
                        retryHazelcastQueue.offer(new RequestHolder(nextRequest.getPayload(), nextRequest.getRetryCount()+1));

                    }
                    //not needed here: calculatorFuture.get();    // innerBlockingQueue.remove(nextRequest);
                } catch (InterruptedException | ExecutionException e) {
//                    e.printStackTrace();// it does not matter, only timeout is interesting, the others could be handled by calculatorFuture
                }
            }
        }, poolMonitor);
    }

    private RequestHolder determineNextRequest() {
        final RequestHolder result;
        final RequestHolder retryRequest = retryHazelcastQueue.poll();
        if (retryRequest != null) {
            result = retryRequest;
        } else {
            result = MyHelper.getNextFromHazelcastETLqueue();
        }
        logger("NEW request: " + result);
        return result;

    }

    private void logger(String log) {
        System.out.println("Thread " + Thread.currentThread().getId() + " " + log);
    }

}

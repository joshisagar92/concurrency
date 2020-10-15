package ch7_calcellation_and_shutdown;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.Socket;
import java.util.concurrent.*;

public class PrimeGenerator {

    private static volatile boolean isCancel;

    public static void main(String[] args) throws InterruptedException {

        new Thread(() -> {
            BigInteger one = BigInteger.ONE;
                while (!isCancel){
                    one = one.nextProbablePrime();
                    System.out.println("Next probable prime = "+ one);
            }
        }).start();

        try {
            Thread.sleep(1000);
        }finally {
            isCancel = true;
        }

    }

    private static ScheduledExecutorService cancService = Executors.newScheduledThreadPool(10);
    private static ExecutorService exeTask = Executors.newFixedThreadPool(10);

    public static void timedRun(Runnable r, long timeout, TimeUnit timeUnit) throws InterruptedException {
        Future<?> task = exeTask.submit(r);

        try {
            task.get(timeout,timeUnit);
        } catch (ExecutionException e) {
            //Exception thrown in task, rethrow
        } catch (TimeoutException e) {
           //Task will be cancelled here.
        }finally {
            //Harmless if task is completed successfully
            task.cancel(true); //Interrupt if running
        }

    }


    interface CancellableTask<T> extends Callable<T>{
        void cancel();
        RunnableFuture<T> newTask();
    }

    public class CancellingExecutor extends ThreadPoolExecutor{

        public CancellingExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
            super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
        }

        protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable){
            if(callable instanceof CancellableTask){
                return ((CancellableTask<T>) callable).newTask();
            }else {
                return super.newTaskFor(callable);
            }
        }
    }

    public abstract class SocketUsingTask<T> implements CancellableTask<T>{

        private Socket socket;

        @Override
        public void cancel() {
            if(socket != null){
                try {
                    socket.close();
                } catch (IOException e) {
                   //Ignore
                }
            }

        }

        @Override
        public RunnableFuture<T> newTask() {
           return new FutureTask<T>(this){
               @Override
               public boolean cancel(boolean mayInterruptIfRunning) {
                   try{
                       SocketUsingTask.this.cancel();
                   }finally {
                        return super.cancel(mayInterruptIfRunning);
                   }
               }
           };
        }
    }


    public class ReaderThread extends Thread {
    }


}

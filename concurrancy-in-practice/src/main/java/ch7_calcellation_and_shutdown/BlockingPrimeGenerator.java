package ch7_calcellation_and_shutdown;

import java.math.BigInteger;
import java.util.concurrent.ArrayBlockingQueue;

public class BlockingPrimeGenerator {
    private static ArrayBlockingQueue<BigInteger> blocking = new ArrayBlockingQueue<BigInteger>(5);
    private static volatile boolean isCancelled;

    public static void main(String[] args) throws InterruptedException {
        new Thread(() -> {
            BigInteger one = BigInteger.ONE;
            while (!isCancelled){
                System.out.println(one + " is added in queue");
                try {
                    blocking.put(one);  // This is the blocking call
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                one = one.nextProbablePrime();
            }
        }).start();

        try {
           for (int i = 0; i <5; i++) {
               Thread.sleep(1000);
               BigInteger take = blocking.take();
               System.out.println(take+" is Removed from queue");
           }
            Thread.sleep(1000);

        }finally {
            isCancelled = true;
        }

    }


}

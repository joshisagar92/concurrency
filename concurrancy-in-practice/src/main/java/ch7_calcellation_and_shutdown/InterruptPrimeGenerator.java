package ch7_calcellation_and_shutdown;

import java.math.BigInteger;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class InterruptPrimeGenerator {
    public static void main(String[] args) throws InterruptedException {
        MyThread myThread = new MyThread();
        myThread.start();

        try {
            Thread.sleep(10000);
        }finally {
            myThread.cancel();
        }
    }
}

class MyThread extends Thread{
    private static ArrayBlockingQueue<BigInteger> blocking =
            new ArrayBlockingQueue<BigInteger>(3);
    @Override
    public void run() {
        BigInteger one = BigInteger.ONE;
            try {

                while (!Thread.currentThread().isInterrupted()){
                    System.out.println(one + " is added in queue");
                    blocking.put(one);  // This is the blocking call
                    one = one.nextProbablePrime();
                }
            } catch (InterruptedException e) {
                //e.printStackTrace();  // Shallow the exception here
            }
            one = one.nextProbablePrime();
    }




    void cancel(){
        interrupt();
    }



}

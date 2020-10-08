package ch3_sharing_object;

public class NoVisibility {

    private static boolean isComplete;;
    private static int counter;

    private static class ReaderThread extends Thread{
        @Override
        public void run() {
            System.out.println("Current thread is "+ Thread.currentThread()+" and waiting to print the counter");
            while (!isComplete){
                //Thread.yield();
                //System.out.println("counter = " + counter);
            }
            System.out.println("counter = " + counter);
        }
    }

    public static void main(String[] args) throws InterruptedException {
        new ReaderThread().start();
        Thread.sleep(5000);  // This timout is creating stale data.
        System.out.println("Changing isComplete..");
        counter = 42;
        isComplete = true;
    }


}

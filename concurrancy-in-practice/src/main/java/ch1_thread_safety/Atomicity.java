package ch1_thread_safety;

public class Atomicity {

    public static void main(String[] args) throws InterruptedException {
        // Scenario 1:
        //Calling is thread safe because we are creating a new object everytime new thread starts
        /*List<SafeCounting> safeCountings = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            SafeCounting safeCounting = new SafeCounting();
            //This operation we can not do in thread as safeCountings is shared mutable object.
            safeCountings.add(safeCounting);
            new Thread(() -> {
                try {
                    safeCounting.service("Hi" );
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
        Thread.sleep(5000);
        System.out.println("safeCountings.size() = " + safeCountings.size());
        long count = safeCountings.stream()
                .mapToLong(safeCounting -> safeCounting.count)
                .reduce(0, (l, l1) -> l + l1);

        System.out.println(count);*/

        //Scenario 2 : when class has a state and state is shared mutable.
        /*SafeCounting safeCounting = new SafeCounting();
        for (int i = 0; i < 500; i++) {
            new Thread(()-> {
                try {
                    safeCounting.service("Hello");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }

        Thread.sleep(5000);
        System.out.println("Count is = "+ safeCounting.count);*/

    }
}

//Following class is Thread safe as far as we call it's method from separate thread by creating a new object.
class SafeCounting {
    long count = 0;
    public void service(String value) throws InterruptedException {
        Thread.sleep(2000);
        count++;
    }
}

class UnsafeSafeCountingWithStaticFeild {
    static long count = 0;
    public void service(String value) throws InterruptedException {
        count++;
        Thread.sleep(2000);
        System.out.println(value);
    }
}




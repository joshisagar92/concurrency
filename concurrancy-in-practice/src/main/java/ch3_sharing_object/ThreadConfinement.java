package ch3_sharing_object;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ThreadConfinement {

    //We are creating a thread local with initial value which will return new
    //person object everytime we call personHolder.get()
    private static ThreadLocal<Person> initialPersonHolder = ThreadLocal
            .withInitial(() -> new Person());

    private static ThreadLocal<Person> personHolder = new ThreadLocal<>();
    public static void main(String[] args) throws InterruptedException {
        //withInitialValue();
        //withSetMethod();
        //withInitialAndSet();

        withImmutability();
    }

    private static void withImmutability() throws InterruptedException {

        new Thread(() ->
        {
            Person p = new Person();
            p.setAge(1);
            personHolder.set(p);
            System.out.println("Object is p ="+p +"and age is"+personHolder.get().age);
        }
        ).start();
        Thread.sleep(1000);
        new Thread(() ->
                {
                    Person p = new Person();
                    p.setAge(2);
                   // personHolder.set(p);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    System.out.println("Object is p ="+p +"and age is"+personHolder.get().age);

                }
                ).start();

        new Thread(() ->
                {
                    Person p = new Person();
                    p.setAge(1);
                    personHolder.set(p);
                    System.out.println("Object is p ="+p +"and age is"+personHolder.get().age);
                }
                ).start();

   //     Thread.sleep(2000);
    //    System.out.println("initialPersonHolder.get().age = " + personHolder.get().age);


    }

    private static void withInitialAndSet() {
        //Object which is last set will be available in initialPersonHolder.get()
        Person p = new Person();
        for (int i = 0; i < 5; i++) {
            System.out.println("Setting person object = "+ p);
            initialPersonHolder.set(p);
            System.out.println("Thread "+Thread.currentThread()+" is holding object ="+ initialPersonHolder.get());
        }
    }

    private static void withSetMethod() {
        Person p = new Person();
        for (int i = 0; i < 5; i++) {
            new Thread(() -> {
                System.out.println("Setting person object = "+ p);
                personHolder.set(p);
                System.out.println("Thread "+Thread.currentThread()+" is holding object ="+ personHolder.get());
            }).start();
        }
    }

    private static void withInitialValue() {
        for (int i = 0; i < 5; i++) {
                new Thread(() -> {
                    System.out.println("Thread "+Thread.currentThread()+" is holding object ="+ initialPersonHolder.get());
                }).start();
        }
    }

}

class Person{
    int age;
    String name;



    public void setAge(int age) {
        this.age = age;
    }
}



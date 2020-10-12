package ch5_building_blocks;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Semaphore;

public class BoundedHashSetTest{
    public static void main(String[] args) throws InterruptedException {
        BoundedHashSet<Integer> hashSet = new BoundedHashSet<>(3);

        hashSet.add(1);
        hashSet.add(2);
        hashSet.add(3);
        hashSet.remove(3);
        hashSet.add(4);
    }
}

class BoundedHashSet<T> {

    private final Set<T> set;
    private final Semaphore semaphore;

    public BoundedHashSet(int bound) {
        this.set = Collections.synchronizedSet(new HashSet<>());
        this.semaphore = new Semaphore(bound);
    }

    public boolean add (T element) throws InterruptedException {
        System.out.println("Before acquire with element "+ element);
        semaphore.acquire();
        System.out.println("After acquire with element "+ element);
        boolean added = false;
        try {
            added = set.add(element);
            return added;
        } finally {
            if(!added){
                semaphore.release();
            }
        }
    }

    public boolean remove(T element){
        boolean remove = set.remove(element);
        if(remove){
            System.out.println("Before releasing element "+ element);
            semaphore.release();
            System.out.println("After releasing element "+ element);
        }
        return remove;
    }
}

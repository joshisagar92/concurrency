package ch5_building_blocks;

import java.util.Map;
import java.util.concurrent.*;

interface Computable<A,V>{

    V compute (A arg) throws InterruptedException;
}

public  class  Memoizer1<A,V> implements Computable<A,V> {

    private final Map<A,V> cache = new ConcurrentHashMap<>();
    private final Computable<A,V> c;

    public Memoizer1(Computable<A, V> c) {
        this.c = c;
    }

    @Override
    public V compute(A arg) throws InterruptedException {

        V result = cache.get(arg);
        if(result == null){
            V compute = c.compute(arg); // There might be chance that compute for same element might be called twice.
            cache.put(arg,compute);
        }

        return null;
    }
}

class  Memoizer3<A,V> implements Computable<A,V> {

    private final Map<A,Future<V>> cache = new ConcurrentHashMap<>();
    private final Computable<A,V> c;

    public Memoizer3(Computable<A, V> c) {
        this.c = c;
    }

    @Override
    public V compute(A arg) throws InterruptedException {
        Future<V> future = cache.get(arg);

        if(future == null){
            FutureTask<V> futureTask = new FutureTask<>(() -> c.compute(arg));
            future = futureTask;
            cache.put(arg,future); // This is the only shared state variable we are modifying.Hence we have to protect in terms of
                                  // thread safety, we can do it by calling putIfAbsent()
            futureTask.run();
        }

        V v = null;
        try {
            v = future.get();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return v;
    }
}

//Correct Implementation
class  Memoizer<A,V> implements Computable<A,V> {

    private final Map<A,Future<V>> cache = new ConcurrentHashMap<>();
    private final Computable<A,V> c;

    public Memoizer(Computable<A, V> c) {
        this.c = c;
    }

    @Override
    public V compute(A arg) throws InterruptedException {
        Future<V> future = cache.get(arg);

        if(future == null){
            FutureTask<V> futureTask = new FutureTask<>(() -> c.compute(arg));
            Future<V> future1 = cache.putIfAbsent(arg, future);
            if(future1 != null){
                 future = futureTask;
                 futureTask.run();
            }
        }
        V v = null;
        try {
            v = future.get();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return v;
    }
}

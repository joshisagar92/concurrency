### SYNCHRONIZED COLLECTIONS
The synchronized collection classes include Vector and Hashtable, part of the original JDK, as well as their cousins added in JDK 1.2, the
synchronized wrapper classes created by the Collections.synchronizedXxx factory methods. These classes achieve thread safety by encapsulating
their state and synchronizing every public method so that only one thread at a time can access the collection state.

#### Problems with Synchronized Collections
The synchronized collections are thread-safe, but you may sometimes need to use additional client-side locking to guard compound actions.
Common compound actions on collections include,
 - iteration 
 - navigation 
 - operations such as put-if-absent
 
 With a synchronized collection, these compound actions are still technically thread-safe even without client-side locking, but they may not
 behave as you might expect when other threads can concurrently modify the collection.
 
These methods seem harmless, and in a sense they are—they can't corrupt the Vector, no matter how many threads call them simultaneously.
But the caller of these methods might have a different opinion. If thread A calls getLast on a Vector with ten elements, thread B calls 
deleteLast on the same Vector, and the operations are interleaved as shown in Figure 5.1, getLast throws ArrayIndexOutOfBoundsException.
These methods seem harmless, and in a sense they are—they can't corrupt the Vector, no matter how many threads call them simultaneously. But
the caller of these methods might have a different opinion. If thread A calls getLast on a Vector with ten elements, thread B calls deleteLast
on the same Vector getLast throws ArrayIndexOutOfBoundsException.

```java
  public static Object getLast(Vector list){
        int lastIndex = list.size() - 1;
        return list.get(lastIndex);
 }
 public static Object deleteLast(Vector list){
    int lastIndex = list.size() - 1;
    return list.remove(lastIndex);
}
```
Between the call to size and the subsequent call to get in getLast, the Vector shrank and the index computed in the first step is no longer 
valid.
By acquiring the collection lock we can make getLast and deleteLast atomic, ensuring that the size of the Vector does not change between call
ing size and get

```java
 public static Object getLast(Vector list){
         synchronized (list){
             int lastIndex = list.size() - 1;
             return list.get(lastIndex);
         }
     }
     public static Object deleteLast(Vector list){
         synchronized (list){
             int lastIndex = list.size() - 1;
             return list.remove(lastIndex);
         }
     }
```
The risk that the size of the list might change between a call to size and the corresponding call to get is also present when we iterate through
the elements of a Vector.

This iteration idiom relies on a leap of faith that other threads will not modify the Vector between the calls to size and get. In a 
single-threaded environment, this assumption is perfectly valid, but when other threads may concurrently modify the Vector it can lead to 
trouble. Just as with getLast, if another thread deletes an element while you are iterating through the Vector and the operations are 
interleaved unluckily, this iteration idiom throws ArrayIndexOutOfBoundsException.

This doesn't mean Vector isn't thread-safe. The state of the Vector is still valid and the exception is in fact in conformance with its 
specification. However, that something as mundane as fetching the last element or iteration throw an exception is clearly undesirable.

```java
for (int i = 0; i < vector.size(); i++) {
        doSomeThing(vector.get(i));
}
```
The problem of unreliable iteration can again be addressed by client-side locking, at some additional cost to scalability.
By holding the Vector lock for the duration of iteration, as shown in Listing 5.4, we prevent other threads from modifying the Vector 
while we are iterating it. Unfortunately, we also prevent other threads from accessing it at all during this time, impairing concurrency

####  Iterators and Concurrentmodificationexception
The iterators returned by the synchronized collections are not designed to deal with concurrent modification, and they are fail-fast—meaning
that if they detect that the collection has changed since iteration began, they throw the unchecked ConcurrentModificationException

These fail-fast iterators are not designed to be foolproof—they are designed to catch concurrency errors on a “good-faith-effort” basis and
thus act only as early-warning indicators for concurrency problems. They are implemented by associating a modification count with the collection
: if the modification count changes during iteration, hasNext or next throws ConcurrentModificationException. However, this check is done 
without synchronization, so there is a risk of seeing a stale value of the modification count and therefore that the iterator does not 
realize a modification has been made. This was a deliberate design tradeoff to reduce the performance impact of the concurrent modification
detection code.

ConcurrentModificationException can arise in single-threaded code as well; this happens when objects are removed from the collection directly
rather than through Iterator.remove.

There are several reasons, however, why locking a collection during iteration may be undesirable. Other threads that need to access the collection
will block until the iteration is complete; if the collection is large or the task performed for each element is lengthy, they could wait a
long time.

Even in the absence of starvation or deadlock risk, locking collections for significant periods of time hurts application scalability. The
longer a lock is held, the more likely it is to be contended, and if many threads are blocked waiting for a lock throughput and CPU utilization
can suffer

An alternative to locking the collection during iteration is to **clone** the collection and iterate the copy instead. Since the clone is 
thread-confined, no other thread can modify it during iteration, eliminating the possibility of ConcurrentModificationException.
(**The collection still must be locked during the clone operation itself.**) Cloning the collection has an obvious performance cost; whether 
this is a favorable tradeoff depends on many factors including the size of the collection, how much work is done for each element, the 
relative frequency of iteration compared to other collection operations, and responsiveness and throughput requirements.

#### Hidden Iterators
While locking can prevent iterators from throwing ConcurrentModificationException, you have to remember to use locking everywhere a shared 
collection might be iterated. This is trickier than it sounds, as iterators are sometimes hidden. There is no explicit iteration in 
HiddenIterator, but the code in bold entails iteration just the same. The string concatenation gets turned by the compiler into a call to 
StringBuilder.append(Object), which in turn invokes the collection's toString method—and the implementation of toString in the standard 
collections iterates the collection and calls toString on each element to produce a nicely formatted representation of the collection's 
contents.

The addTenThings method could throw ConcurrentModificationException, because the collection is being iterated by toString in the process of 
preparing the debugging message. Of course, the real problem is that HiddenIterator is not thread-safe; the HiddenIterator lock should be 
acquired before using set in the println call, but debugging and logging code commonly neglect to do this.

The real lesson here is that the greater the distance between the state and the synchronization that guards it, the more likely that someone
will forget to use proper synchronization when accessing that state. If HiddenIterator wrapped the HashSet with a synchronizedSet, encapsulating
the synchronization, this sort of error would not occur.



```java
class HiddenIterator{
    private final Set<Integer> set = new HashSet<>();

    public synchronized void add(Integer i){
        set.add(i);
    }

    public synchronized void remove(Integer i){
        set.remove(i);
    }

    public void addThings(){
        Random r = new Random();
        for (int i = 0; i < 10; i++) {
            add(r.nextInt());
        }

        System.out.println("DEBUG : Added 10 elements to set " + set);

    }
}
```
Iteration is also indirectly invoked by the collection's hashCode and equals methods, which may be called if the collection is used as an 
element or key of another collection. Similarly, the containsAll, removeAll, and retainAll methods, as well as the constructors that take 
collections as arguments, also iterate the collection. All of these indirect uses of iteration can cause ConcurrentModificationException.

### CONCURRENT COLLECTIONS
Java 5.0 improves on the synchronized collections by providing several concurrent collection classes. Synchronized collections achieve their
thread safety by serializing all access to the collection's state. The cost of this approach is poor concurrency; when multiple threads
contend for the collection-wide lock, throughput suffers.

The concurrent collections, on the other hand, are designed for concurrent access from multiple threads. Java 5.0 adds ConcurrentHashMap,
a replacement for synchronized hash-based Map implementations, and CopyOnWriteArrayList, a replacement for synchronized List implementations
for cases where traversal is the dominant operation. The new ConcurrentMap interface adds support for common compound actions such as 
put-if-absent, replace, and conditional remove.

Replacing synchronized collections with concurrent collections can offer dramatic scalability improvements with little risk.
Java 5.0 also adds two new collection types, Queue and BlockingQueue. A Queue is intended to hold a set of elements temporarily while they
await processing. Several implementations are provided, including ConcurrentLinkedQueue, a traditional FIFO queue, and PriorityQueue,
a (non concurrent) priority ordered queue. Queue operations do not block; if the queue is empty, the retrieval operation returns null.
While you can simulate the behavior of a Queue with a List—in fact, LinkedList also implements Queue—**the Queue classes were added because
eliminating the random-access requirements of List admits more efficient concurrent implementations.**

Just as ConcurrentHashMap is a concurrent replacement for a synchronized hash-based Map, Java 6 adds ConcurrentSkipListMap and ConcurrentSkipListSet,
which are concurrent replacements for a synchronized SortedMap or SortedSet.

#### ConcurrentHashMap
The synchronized collections classes hold a lock for the duration of each operation. Some operations, such as HashMap.get or List.contains,
may involve more work than is initially obvious: traversing a hash bucket or list to find a specific object entails calling equals (which 
itself may involve a fair amount of computation) on a number of candidate objects. In a hash-based collection, if hashCode does not spread 
out hash values well, elements may be unevenly distributed among buckets; in the degenerate case, a poor hash function will turn a hash 
table into a linked list. Traversing a long list and calling equals on some or all of the elements can take a long time, and during that 
time no other thread can access the collection.

ConcurrentHashMap is a hash-based Map like HashMap, but it uses an entirely different locking strategy that offers better concurrency and
scalability. Instead of synchronizing every method on a common lock, restricting access to a single thread at a time, it uses a finer-grained
locking mechanism called lock striping.

ConcurrentHashMap, along with the other concurrent collections, further improve on the synchronized collection classes by providing 
iterators that do not throw ConcurrentModificationException, thus eliminating the need to lock the collection during iteration. 
The iterators returned by ConcurrentHashMap are weakly consistent instead of fail-fast. A weakly consistent iterator can tolerate 
concurrent modification, traverses elements as they existed when the iterator was constructed, and may (but is not guaranteed to) reflect
modifications to the collection after the construction of the iterator.

As with all improvements, there are still a few tradeoffs. The semantics of methods that operate on the entire Map, such as size and isEmpty,
have been slightly weakened to reflect the concurrent nature of the collection. Since the result of size could be out of date by the time it
is computed, it is really only an estimate, so size is allowed to return an approximation instead of an exact count. While at first this may
seem disturbing, in reality methods like size and isEmpty are far less useful in concurrent environments because these quantities are moving
targets. So the requirements for these operations were weakened to enable performance optimizations for the most important operations,
primarily get, put, containsKey, and remove.

The one feature offered by the synchronized Map implementations but not by ConcurrentHashMap is the ability to lock the map for exclusive
access. With Hashtable and synchronizedMap, acquiring the Map lock prevents any other thread from accessing it. This might be necessary in
unusual cases such as adding several mappings atomically, or iterating the Map several times and needing to see the same elements in the 
same order.Because it has so many advantages and so few disadvantages compared to Hashtable or synchronizedMap, replacing synchronized Map 
implementations with ConcurrentHashMap in most cases results only in better scalability.

#### Additional Atomic Map Operations
Since a ConcurrentHashMap cannot be locked for exclusive access, we cannot use client-side locking to create new atomic operations such as
put-if-absent, as we did for Vector. Instead, a number of common compound operations such as put-if-absent, remove-if-equal,
and replace-if-equal are implemented as atomic operations and specified by the ConcurrentMap interface. If you find
yourself adding such functionality to an existing synchronized Map implementation, it is probably a sign that you should consider using a
ConcurrentMap instead.     

#### CopyOnWriteArrayList
CopyOnWriteArrayList is a concurrent replacement for a synchronized List that offers better concurrency in some common situations and 
eliminates the need to lock or copy the collection during iteration.

The copy-on-write collections derive their thread safety from the fact that as long as an effectively immutable object is properly published,
no further synchronization is required when accessing it. They implement mutability by creating and republishing a new copy of the collection
every time it is modified. Iterators for the copy-on-write collections retain a reference to the backing array that was current at the start
of iteration, and since this will never change, they need to synchronize only briefly to ensure visibility of the array contents. As a result,\
multiple threads can iterate the collection without interference from one another or from threads wanting to modify the collection.
The iterators returned by the copy-on-write collections do not throw ConcurrentModificationException and return the elements exactly as they
were at the time the iterator was created, regardless of subsequent modifications.

Obviously, there is some cost to copying the backing array every time the collection is modified, especially if the collection is large;
the copy-on-write collections are reasonable to use only when iteration is far more common than modification. This criterion exactly
describes many event-notification systems: delivering a notification requires iterating the list of registered listeners and calling each 
one of them, and in most cases registering or unregistering an event listener is far less common than receiving an event notification.

### BLOCKING QUEUES AND THE PRODUCER-CONSUMER PATTERN
Blocking queues provide blocking put and take methods as well as the timed equivalents offer and poll. If the queue is full, put blocks until
space becomes available; if the queue is empty, take blocks until an element is available. Queues can be bounded or unbounded; unbounded
queues are never full, so a put on an unbounded queue never blocks.

The familiar division of labor for two people washing the dishes is an example of a producer-consumer design: one person washes the dishes
and places them in the dish rack, and the other person retrieves the dishes from the rack and dries them. In this scenario, the dish rack
acts as a blocking queue; if there are no dishes in the rack, the consumer waits until there are dishes to dry, and if the rack fills up,
the producer has to stop washing until there is more space. This analogy extends to multiple producers (though there may be contention
for the sink) and multiple consumers; each worker interacts only with the dish rack. No one needs to know how many producers or consumers
there are, or who produced a given item of work.

Blocking queues also provide an offer method, which returns a failure status if the item cannot be enqueued. This enables you to create more
flexible policies for dealing with overload, such as shedding load, serializing excess work items and writing them to disk, reducing the
number of producer threads, or throttling producers in some other manner.

The class library contains several implementations of BlockingQueue. LinkedBlockingQueue and ArrayBlockingQueue are FIFO queues, analogous 
to LinkedList and ArrayList but with better concurrent performance than a synchronized List. PriorityBlockingQueue is a priority-ordered
queue, which is useful when you want to process elements in an order other than FIFO. Just like other sorted collections,
PriorityBlockingQueue can compare elements according to their natural order (if they implement Comparable) or using a Comparator.

#### Example: Desktop Search

- DiskCrawler : a producer task that searches a file hierarchy for files meeting an indexing criterion and puts their names on the work queue;
- Indexer : shows the consumer task that takes file names from the queue and indexes them.

The producer-consumer pattern offers a thread-friendly means of decomposing the desktop search problem into simpler components. Factoring file
-crawling and indexing into separate activities results in code that is more readable and reusable than with a monolithic activity that does
both; each of the activities has only a single task to do, and the blocking queue handles all the flow control, so the code for each is 
simpler and clearer.

The producer-consumer pattern also enables several performance benefits. Producers and consumers can execute concurrently; if one is I/O-bound
and the other is CPU-bound, executing them concurrently yields better overall throughput than executing them sequentially. If the producer 
and consumer activities are parallelizable to different degrees, tightly coupling them reduces parallelizability to that of the less parallelizable
activity.

#### Serial Thread Confinement
For mutable objects, producer-consumer designs and blocking queues facilitate serial thread confinement for handing off ownership of objects
from producers to consumers. A thread-confined object is owned exclusively by a single thread, but that ownership can be “transferred” by
publishing it safely where only one other thread will gain access to it and ensuring that the publishing thread does not access it after the
handoff. The safe publication ensures that the object's state is visible to the new owner, and since the original owner will not touch it 
again, it is now confined to the new thread. The new owner may modify it freely since it has exclusive access.

Object pools exploit serial thread confinement, “lending” an object to a requesting thread. As long as the pool contains sufficient internal
synchronization to publish the pooled object safely, and as long as the clients do not themselves publish the pooled object or use it after
returning it to the pool, ownership can be transferred safely from thread to thread.

One could also use other publication mechanisms for transferring ownership of a mutable object, but it is necessary to ensure that only one
thread receives the object being handed off. Blocking queues make this easy; with a little more work, it could also done with the atomic
remove method of ConcurrentMap or the compareAndSet method of AtomicReference.

#### Deques and Work Stealing
Java 6 also adds another two collection types, Deque (pronounced “deck”) and BlockingDeque, that extend Queue and BlockingQueue. A Deque is
a doubleended queue that allows efficient insertion and removal from both the head and the tail. Implementations include ArrayDeque and 
LinkedBlockingDeque.

Just as blocking queues lend themselves to the producer-consumer pattern, deques lend themselves to a related pattern called work stealing.
A producerconsumer design has one shared work queue for all consumers; in a work stealing design, every consumer has its own deque. If a 
consumer exhausts the work in its own deque, it can steal work from the tail of someone else's deque. Work stealing can be more scalable 
than a traditional producer-consumer design because workers don't contend for a shared work queue; most of the time they access only their 
own deque, reducing contention. When a worker has to access another's queue, it does so from the tail rather than the head, further reducing
contention.

#### BLOCKING AND INTERRUPTIBLE METHODS
The put and take methods of BlockingQueue throw the checked InterruptedException, as do a number of other library methods such as Thread.sleep.
When a method can throw InterruptedException, it is telling you that it is a blocking method, and further that if it is interrupted, it will make
an effort to stop blocking early.

Thread provides the interrupt method for interrupting a thread and for querying whether a thread has been interrupted. Each thread has a
boolean property that represents its interrupted status; interrupting a thread sets this status.

Interruption is a cooperative mechanism. One thread cannot force another to stop what it is doing and do something else; when thread A
interrupts thread B, A is merely requesting that B stop what it is doing when it gets to a convenient stopping point—if it feels like it.
While there is nothing in the API or language specification that demands any specific application-level semantics for interruption, the
most sensible use for interruption is to cancel an activity. Blocking methods that are responsive to interruption make it easier to cancel
long-running activities on a timely basis.

When your code calls a method that throws InterruptedException, then your method is a blocking method too, and must have a plan for responding to
interruption. For library code, there are basically two choices:

- Propagate the InterruptedException.
- Restore the interrupt.

for instance when your code is part of a Runnable. In these situations, you must catch InterruptedException and restore the interrupted
status by calling interrupt on the current thread, so that code higher up the call stack can see that an interrupt was issued.

 But there is one thing you should not do with InterruptedException—catch it and do nothing in response. This deprives code higher up on
 the call stack of the opportunity to act on the interruption, because the evidence that the thread was interrupted is lost.
 
 ### SYNCHRONIZERS
 A synchronizer is any object that **coordinates the control flow of threads based on its state**. Blocking queues can act as synchronizers;
 other types of synchronizers include semaphores, barriers, and latches.
 
 All synchronizers share certain structural properties: they encapsulate state that determines whether threads arriving at the synchronizer
 should be allowed to pass or forced to wait, provide methods to manipulate that state, and provide methods to wait efficiently for the
 synchronizer to enter the desired state.
 
 #### Latches
 A latch is a synchronizer that can delay the progress of threads until it reaches its terminal state . A latch acts as a gate:
 until the latch reaches the terminal state the gate is closed and no thread can pass, and in the terminal state the gate opens, allowing
 all threads to pass. Once the latch reaches the terminal state, it cannot change state again, so it remains open forever. Latches can be
 used to ensure that certain activities do not proceed until other one-time activities complete.
 
 CountDownLatch is a flexible latch implementation that can be used in any of these situations; it allows one or more threads to wait for a
 set of events to occur. The latch state consists of a counter initialized to a positive number, representing the number of events to wait
 for. The countDown method decrements the counter, indicating that an event has occurred, and the await methods wait for the counter to
 reach zero, which happens when all the events have occurred. If the counter is nonzero on entry, await blocks until the counter reaches
 zero, the waiting thread is interrupted, or the wait times out.
 
 [***TestHarness***](TestHarness.java) illustrates two common uses for latches. TestHarness creates a number of threads that run a given task concurrently.
 It uses two latches, a “starting gate” and an “ending gate”. The starting gate is initialized with a count of one; the ending gate is
 initialized with a count equal to the number of worker threads. The first thing each worker thread does is wait on the starting gate;
 this ensures that none of them starts working until they all are ready to start. The last thing each does is count down on the ending gate;
 this allows the master thread to wait efficiently until the last of the worker threads has finished, so it can calculate the elapsed time.
 
 #### FutureTask
 A computation represented by a FutureTask is implemented with a Callable, the result-bearing equivalent of Runnable, and can be in one of three states:
 waiting to run, running, or completed. Once a FutureTask enters the completed state, it stays in that state forever.
  
 The behavior of Future.get depends on the state of the task. If it is completed, get returns the result immediately,
 and otherwise blocks until the task transitions to the completed state and then returns the result or throws an 
 exception. FutureTask conveys the result from the thread executing the computation to the thread(s) retrieving the
 result; the specification of FutureTask guarantees that this transfer constitutes a safe publication of the result.
 
 FutureTask is used by the Executor framework to represent asynchronous tasks, and can also be used to represent any potentially lengthy computation
 that can be started before the results are needed.
 
 [***PreLoader***](PreLoader.java)
 
 Preloader creates a FutureTask that describes the task of loading product information from a database and a thread in which the computation
 will be performed. It provides a start method to start the thread, since it is inadvisable to start a thread from a constructor or static 
 initializer. When the program later needs the ProductInfo, it can call get, which returns the loaded data if it is ready, or waits for the
 load to complete if not.
 
 #### Semaphores
 Counting semaphores are used to control the number of activities that can access a certain resource or perform a given action at the same
 time.Counting semaphores can be used to implement resource pools or to impose a bound on a collection.
 
 A Semaphore manages a set of virtual permits; the initial number of permits is passed to the Semaphore constructor. Activities can acquire
 permits (as long as some remain) and release permits when they are done with them. If no permit is available, acquire blocks until one is
 (or until interrupted or the operation times out). The release method returns a permit to the semaphore.
 
 A degenerate case of a counting semaphore is a binary semaphore, a Semaphore with an initial count of one. A binary semaphore can be used 
 as a mutex with nonreentrant locking semantics; whoever holds the sole permit holds the mutex.
 
 Similarly, you can use a Semaphore to turn any collection into a blocking bounded collection, as illustrated by
 [***BoundedHashSet***](BoundedHashSetTest.java)
 
 #### Barriers 
 We have seen how latches can facilitate starting a group of related activities or waiting for a group of related activities to complete. 
 Latches are single-use objects; once a latch enters the terminal state, it cannot be reset.
 
 Barriers are similar to latches in that they block a group of threads until some event has occurred. The key difference is that with a barrier,
 all the threads must come together at a barrier point at the same time in order to proceed. Latches are for waiting for events; barriers
 are for waiting for other threads. A barrier implements the protocol some families use to rendezvous during a day at the mall: “Everyone meet
 at McDonald's at 6:00; once you get there, stay there until everyone shows up, and then we'll figure out what we're doing next.”
 
 CyclicBarrier allows a fixed number of parties to rendezvous repeatedly at a barrier point and is useful in parallel iterative algorithms
 that break down a problem into a fixed number of independent subproblems. Threads call await when they reach the barrier point, and await
 blocks until all the threads have reached the barrier point. If all threads meet at the barrier point, the barrier has been successfully
 passed, in which case all threads are released and the barrier is reset so it can be used again. If a call to await times out or a thread
 blocked in await is interrupted, then the barrier is considered broken and all outstanding calls to await terminate with BrokenBarrierException.
 If the barrier is successfully passed, await returns a unique arrival index for each thread, which can be used to “elect” a leader that
 takes some special action in the next iteration.
 
 Barriers are often used in simulations, where the work to calculate one step can be done in parallel but all the work associated with a given 
 step must complete before advancing to the next step.
 
 ### BUILDING AN EFFICIENT, SCALABLE RESULT CACHE
 
 [***Memoizer1***](Memoizer1.java)
 
    



 

    

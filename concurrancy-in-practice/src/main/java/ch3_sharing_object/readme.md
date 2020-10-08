We have seen how synchronized blocks and methods can ensure that operations execute atomically, but it is a common 
misconception that synchronized is only about atomicity or demarcating “critical sections”. Synchronization also has 
another significant, and subtle, aspect: memory visibility. We want not only to prevent one thread from modifying 
the state of an object when another is using it, but also to ensure that when a thread modifies the state of an object, 
other threads can actually see the changes that were made. 

### VISIBILITY
Visibility is subtle because the things that can go wrong are so counterintuitive.It may be hard to accept at first, but
when the reads and writes occur in different threads, this is simply not the case. In general, there is no guarantee that
the reading thread will see a value written by another thread on a timely basis, or even at all. In order to ensure 
visibility of memory writes across threads, you must use synchronization.
[***NoVisibility***](NoVisibility.java) illustrates what can go wrong when threads share data without synchronization.

This may seem like a broken design, but it is meant to allow JVMs to take full advantage of the performance of modern 
multiprocessor hardware. For example, in the absence of synchronization, the Java Memory Model permits the compiler to 
reorder operations and cache values in registers, and permits CPUs to reorder operations and cache values in 
processor-specific caches.

#### Stale Data
NoVisibility demonstrated one of the ways that insufficiently synchronized programs can cause surprising results: stale data
Stale data can cause serious and confusing failures such as unexpected exceptions, corrupted data structures,
inaccurate computations, and infinite loops.
Reading data without synchronization is analogous to using the READ_UNCOMMITTED isolation level in a database, where you
are willing to trade accuracy for performance. However, in the case of unsynchronized reads, you are trading away a 
greater degree of accuracy, since the visible value for a shared variable can be arbitrarily stale.

[***MutableInteger***](MutableInteger.java)  is not thread-safe because the value field is accessed from both get and set without
synchronization. Among other hazards, it is susceptible to stale values: if one thread calls set, other threads calling 
get may or may not see that update.

We can make [***MutableInteger***](MutableInteger.java) thread safe by synchronizing the getter and setter as shown in 
[***SynchronizedInteger***](SynchronizedInteger.java) Synchronizing only the setter would not be sufficient: threads calling get would still be able to 
see stale values.

#### Nonatomic 64-bit Operations
When a thread reads a variable without synchronization, it may see a stale value, but at least it sees a value that was
actually placed there by some thread rather than some random value. This safety guarantee is called **out-of-thin-air** safety.

Out-of-thin-air safety applies to all variables, with one exception: 64-bit numeric variables (double and long) that
are not declared volatile.
The Java Memory Model requires fetch and store operations to be atomic, but for nonvolatile long and double variables,
the JVM is permitted to treat a **64-bit read or write as two separate 32-bit operations**.
If the reads and writes occur in different threads, it is therefore possible to read a nonvolatile long and get back 
the **high 32 bits of one value and the low 32 bits of another**.Thus, even if you don't care about stale values, it is not 
safe to use shared mutable long and double variables in multithreaded programs unless they are declared
volatile or guarded by a lock

#### Locking and Visibility
Intrinsic locking can be used to guarantee that one thread sees the effects of another in a predictable manner, as
.When thread A executes a synchronized block, and subsequently thread B enters a synchronized block guarded by the same
lock, the values of variables that were visible to A prior to releasing the lock are guaranteed to be visible to B upon 
acquiring the lock. In other words, everything A did in or prior to a synchronized block is visible to B when it executes
a synchronized block guarded by the same lock. Without synchronization, there is no such guarantee.
e can now give the other reason for the rule requiring all threads to synchronize on the same lock when accessing a shared
mutable variable—to guarantee that values written by one thread are made visible to other threads. Otherwise,
if a thread reads a variable without holding the appropriate lock, it might see a stale value.
Locking is not just about mutual exclusion; it is also about memory visibility. To ensure that all threads see the most
up-to-date values of shared mutable variables, the reading and writing threads must synchronize on a common lock.

#### Volatile Variables
The Java language also provides an alternative, weaker form of synchronization, volatile variables, to ensure that 
updates to a variable are propagated predictably to other threads. When a field is declared volatile, the compiler and 
runtime are put on notice that this variable is shared and that operations on it should not be reordered with other 
memory operations.
Good uses of volatile variables include ensuring the visibility of their own state, that of the object they refer to, or
indicating that an important lifecycle event (such as initialization or shutdown) has occurred.

For this example to work, the asleep flag must be volatile. Otherwise, the thread might not notice when asleep has been 
set by another thread. We could instead have used locking to ensure visibility of changes to asleep, but that would have
made the code more cumbersome.

```java
volatile boolean asleep;

while(!asleep){
    countSomeSheep()
}
```

For server applications, be sure to always specify the -server JVM command line switch when invoking the JVM, even for
development and testing. The server JVM performs more optimization than the client JVM, such as hoisting variables out 
of a loop that are not modified in the loop; code that might appear to work in the development environment (client JVM) 
can break in the deployment environment (server JVM). For example, had we “forgotten” to declare the variable asleep as 
volatile, the server JVM could hoist the test out of the loop (turning it into an infinite loop), but the
client JVM would not. An infinite loop that shows up in development is far less costly than one that only shows up in production.

_**Locking can guarantee both visibility and atomicity; volatile variables can only guarantee visibility.**_
You can use volatile variables only when all the following criteria are met,
- Writes to the variable do not depend on its current value, or you can ensure that only a single thread ever updates the value;
- Writes to the variable do not depend on its current value, or you can ensure that only a single thread ever updates the value;
- Locking is not required for any other reason while the variable is being accessed.

### PUBLICATION AND ESCAPE
Publishing an object means making it available to code outside of its current scope, such as by storing a reference to 
it where other code can find it, returning it from a nonprivate method, or passing it to a method in another class.
Publishing internal state variables can compromise encapsulation and make it more difficult to preserve invariants;
publishing objects before they are fully constructed can compromise thread safety. An object that is published when it 
should not have been is said to have escaped. 
he most blatant form of publication is to store a reference in a public static field, where any class and thread could 
see it. The initialize method instantiates a new HashSet and publishes it by storing a reference to it into knownSecrets.

```java
public static Set<Secret> knownSecret;
public void initialize(){
    knownSecret = new HashSet<>();
}
```
Publishing one object may indirectly publish others. If you add a Secret to the published knownSecrets set, you've also
published that Secret, because any code can iterate the Set and obtain a reference to the new Secret. Similarly,
returning a reference from a nonprivate method also publishes the returned object. UnsafeStates 

```java
class UnsafeState{
private String[] states = new String[ ]{
"AK","AL",..
}
public String[] getStates(){
    return states
}
}
```
Publishing states in this way is problematic because any caller can modify its contents. In this case, the states array
has escaped its intended scope, because what was supposed to be private state has been effectively made public.

Once an object escapes, you have to assume that another class or thread may, maliciously or carelessly, misuse it.
This is a compelling reason to use encapsulation: it makes it practical to analyze programs for correctness and harder
to violate design constraints accidentally.

A final mechanism by which an object or its internal state can be published is to publish an inner class instance, as shown

```java
public class ThisEscape{
    public ThisEscape(EventSource source){
        source.registerListner(
            new EventListener(){
                    public void onEvent(Event e){
                                    doSomething(e);
                                }   
                }           
        )
    }   
}
```
When ThisEscape publishes the EventListener, it implicitly publishes the enclosing ThisEscape instance as well, because 
inner class instances contain a hidden reference to the enclosing instance.

####  Safe Construction Practices
More specifically, the this reference should not escape from the thread until after the constructor returns. The this 
reference can be stored somewhere by the constructor so long as it is not used by another thread until after construction.
A common mistake that can let the this reference escape during construction is to start a thread from a constructor.
When an object creates a thread from its constructor, it almost always shares its this reference with the new thread,
either explicitly (by passing it to the constructor) or implicitly (because the Thread or Runnable is an inner class of
the owning object). The new thread might then be able to see the owning object before it is fully constructed.
There's nothing wrong with creating a thread in a constructor, but it is best not to start the thread immediately.
Instead, expose a start or initialize method that starts the owned thread.

### THREAD CONFINEMENT
Accessing shared, mutable data requires using synchronization; one way to avoid this requirement is to not share.
If data is only accessed from a single thread, no synchronization is needed. This technique, thread confinement,
is one of the simplest ways to achieve thread safety. When an object is confined to a thread,
such usage is automatically thread-safe even if the confined object itself is not.
application of thread confinement is the use of pooled JDBC (Java Database Connectivity) Connection objects.

The JDBC specification does not require that Connection objects be thread-safe. In typical server applications, a thread
acquires a connection from the pool, uses it for processing a single request, and returns it. Since most requests,
such as servlet requests or EJB (Enterprise JavaBeans) calls, are processed synchronously by a single thread,
and the pool will not dispense the same connection to another thread until it has been returned,
this pattern of connection management implicitly confines the Connection to that thread for the duration of the request.
**_it is the programmer's responsibility to ensure that thread-confined objects do not escape from their intended thread._**

#### Ad-hoc Thread Confinement
Ad-hoc thread confinement describes when the responsibility for maintaining thread confinement falls entirely on the implementation.
A special case of thread confinement applies to volatile variables. It is safe to perform read-modify-write operations 
on shared volatile variables as long as you ensure that the volatile variable is only written from a single thread.
In this case, you are confining the modification to a single thread to prevent race conditions,
and the visibility guarantees for volatile variables ensure that other threads see the most up-to-date value.

#### Stack Confinement
Stack confinement is a special case of thread confinement in which an object can only be reached through local variables.
Just as encapsulation can make it easier to preserve invariants, local variables can make it easier to confine objects to
a thread. Local variables are intrinsically confined to the executing thread; they exist on the executing thread's stack,
which is not accessible to other threads. Stack confinement (also called within-thread or thread-local usage,
but not to be confused with the ThreadLocal library class) is simpler to maintain and less fragile than ad-hoc thread confinement.

For primitively typed local variables you cannot violate stack confinement even if you tried. There is no way to obtain 
a reference to a primitive variable, so the language semantics ensure that primitive local variables are always stack confined.
Maintaining stack confinement for object references requires a little more assistance from the programmer to ensure that
the referent does not escape.

#### ThreadLocal
A more formal means of maintaining thread confinement is ThreadLocal, which allows you to associate a per-thread value
with a value-holding object. Thread-Local provides get and set accessormethods that maintain a separate copy of the
value for each thread that uses it, so a get returns the most recent value passed to set from the currently executing thread.

Thread-local variables are often used to prevent sharing in designs based on mutable Singletons or global variables.
For example, a single-threaded application might maintain a global database connection that is initialized at startup
to avoid having to pass a Connection to every method. Since JDBC connections may not be thread-safe, a multithreaded
application that uses a global connection without additional coordination is not thread-safe either.

If you are porting a single-threaded application to a multithreaded environment, you can preserve thread safety by 
converting shared global variables into ThreadLocals, if the semantics of the shared globals permits this; 
an applicationwide cache would not be as useful if it were turned into a number of thread-local caches.

It is easy to abuse ThreadLocal by treating its thread confinement property as a license to use global variables or as a
means of creating “hidden” method arguments. Like global variables, thread-local variables can detract from reusability
and introduce hidden couplings among classes, and should therefore be used with care.

[***code***](ThreadConfinement.java) 

### IMMUTABILITY
An immutable object is one whose state cannot be changed after construction. Immutable objects are inherently thread-safe;
their invariants are established by the constructor, and if their state cannot be changed, these invariants always hold.
Immutable objects are always thread-safe.   
Immutable objects are simple. They can only be in one state, which is carefully controlled by the constructor.
One of the most difficult elements of program design is reasoning about the possible states of complex objects.
Reasoning about the state of immutable objects, on the other hand, is trivial.
Immutable objects are also safer. Passing a mutable object to untrusted code, or otherwise publishing it where untrusted
code could find it, is dangerous—the untrusted code might modify its state, or, worse, retain a reference to it and 
modify its state later from another thread. On the other hand, immutable objects cannot be subverted in this manner by 
malicious or buggy code, so they are safe to share and publish freely without the need to make defensive copies 
Neither the Java Language Specification nor the Java Memory Model formally defines immutability, but immutability is
not equivalent to simply declaring all fields of an object final. An object whose fields are all final may still be mutable,
since final fields can hold references to mutable objects.

An object is immutable if:
- Its state cannot be modifled after construction;
- All its flelds are final
- It is properly constructed (the this reference does not escape during construction).

Because program state changes all the time, you might be tempted to think that immutable objects are of limited use,
but this is not the case. There is a difference between an object being immutable and the reference to it being immutable.
Program state stored in immutable objects can still be updated by “replacing” immutable objects with a new instance
holding new state; the next section offers an example of this technique.
Many developers fear that this approach will create performance problems, but these fears are usually unwarranted.
Allocation is cheaper than you might think, and immutable objects offer additional performance advantages such as reduced
need for locking or defensive copies and reduced impact on generational garbage collection.

#### Final Fields   
The final keyword, a more limited version of the const mechanism from C++, supports the construction of immutable objects.
Final fields can't be modified (although the objects they refer to can be modified if they are mutable), but they also 
have special semantics under the Java Memory Model. It is the use of final fields that makes possible the guarantee of 
initialization safety that lets immutable objects be freely accessed and shared without synchronization.
Even if an object is mutable, making some fields final can still simplify reasoning about its state, since limiting the
mutability of an object restricts its set of possible states. An object that is “mostly immutable” but has one or two 
mutable state variables is still simpler than one that has many mutable variables. Declaring fields final also documents 
to maintainers that these fields are not expected to change.Just as it is a good practice to make all fields private 
unless they need greater visibility , it is a good practice to make all fields final unless they need to
be mutable.

####  Example: Using Volatile to Publish Immutable Objects
[***Code***]() 

### SAFE PUBLICATION
So far we have focused on ensuring that an object not be published, such as when it is supposed to be confined to a thread
or within another object. Of course, sometimes we do want to share objects across threads, and in this case we must do so
safely. Unfortunately, simply storing a reference to an object into a public field is not enough to
publish that object safely.

```java
public Holder holder;
public void initialize(){
    holder = new Holder(45);
}
```

You may be surprised at how badly this harmless-looking example could fail. Because of visibility problems, the Holder
could appear to another thread to be in an inconsistent state, even though its invariants were properly established by 
its constructor! This improper publication could allow another thread to observe a partially constructed object.

####  Improper Publication: When Good Objects Go Bad
You cannot rely on the integrity of partially constructed objects. An observing thread could see the object in an 
inconsistent state, and then later see its state suddenly change, even though it has not been modified since publication.
The problem here is not the Holder class itself, but that the Holder is not properly published. However,
Holder can be made immune to improper publication by declaring the n field to be final, which would make Holder immutable

```java
class Holder{
    public int n;

    public Holder(int n) {
        this.n = n;
    }

    public void assertSanity(){

        if (n != n){
            throw new AssertionError();
        }
    }
}

Holder holder = new Holder(45);
``` 
Because synchronization was not used to make the Holder visible to other threads, we say the Holder was not properly 
published. Two things can go wrong with improperly published objects.
- Other threads could see a stale value for the holder field, and thus see a null reference or other older value even 
  though a value has been placed in holder.
- But far worse, other threads could see an up-todate value for the holder reference, but stale values for the state of 
  the Holder.  
While it may seem that field values set in a constructor are the first values written to those fields and therefore that
there are no “older” values to see as stale values, the Object constructor first writes the default values to all fields
before subclass constructors run. It is therefore possible to see the default value for a field as a stale value.  

#### Immutable Objects and Initialization Safety
Because immutable objects are so important, the JavaMemory Model offers a special guarantee of initialization safety for
sharing immutable objects. As we've seen, that an object reference becomes visible to another thread does not necessarily
mean that the state of that object is visible to the consuming thread. In order to guarantee a consistent view of the 
object's state, synchronization is needed.

Immutable objects, on the other hand, can be safely accessed even when synchronization is not used to publish the object
reference. For this guarantee of initialization safety to hold, all of the requirements for immutability must be met: 
unmodi-fiable state, all fields are final, and proper construction.

Immutable objects can be used safely by any thread without additional synchronization, even when synchronization is not
used to publish them.

This guarantee extends to the values of all final fields of properly constructed objects; final fields can be safely 
accessed without additional synchronization. However, if final fields refer to mutable objects, synchronization is still
required to access the state of the objects they refer to.

This guarantee extends to the values of all final fields of properly constructed objects; final fields can be safely accessed
without additional synchronization. However, if final fields refer to mutable objects, synchronization is still required
to access the state of the objects they refer to. 
#### Safe Publication Idioms
To publish an object safely, both the reference to the object and the object's state must be made visible to other threads
at the same time. A properly constructed object can be safely published by:

- Initializing an object reference from a static initializer
- Storing a reference to it into a volatile field or AtomicReference
- Storing a reference to it into a final field of a properly constructed object
- Storing a reference to it into a field that is properly guarded by a lock

The internal synchronization in thread-safe collections means that placing an object in a thread-safe collection, such 
as a Vector or synchronizedList, fulfills the last of these requirements. If thread A places object X in a thread-safe
collection and thread B subsequently retrieves it, B is guaranteed to see the state of X as A left it, even though the 
application code that hands X off in this manner has no explicit synchronization. The thread-safe library collections 
offer the following safe publication guarantees, even if the Javadoc is less than clear on the subject:

- Placing a key or value in a Hashtable, synchronizedMap, or Concurrent-Map safely publishes it to any thread that retrieves
it from the Map (whether directly or via an iterator);
- Placing an element in a Vector, CopyOnWriteArrayList, CopyOnWrite-ArraySet, synchronizedList, or synchronizedSet safely 
publishes it to any thread that retrieves it from the collection;
- Placing an element on a BlockingQueue or a ConcurrentLinkedQueue safely publishes it to any thread that retrieves it from
the queue. 
Using a static initializer is often the easiest and safest way to publish objects that can be statically constructed:
```java
public static Holder holder = new Holder(42);
```
#### Effectively immutable objects
Safe publication is sufficient for other threads to safely access objects that are not going to be modified after publication
without additional synchronization. The safe publication mechanisms all guarantee that the as-published state of an object
is visible to all accessing threads as soon as the reference to it is visible, and if that state is not going to be changed again,
this is sufficient to ensure that any access is safe.

Objects that are not technically immutable, but whose state will not be modified after publication, are called effectively
immutable. They do not need to meet the strict definition of immutability they merely need to be treated
by the program as if they were immutable after they are published. Using effectively immutable objects can simplify
development and improve performance by reducing the need for synchronization.

Safely published effectively immutable objects can be used safely by any thread without additional synchronization.

For example, Date is mutable, but if you use it as if it were immutable, you may be able to eliminate the locking that
would otherwise be required when sharing a Date across threads. Suppose you want to maintain a Map storing the last login
time of each user:

```java
//We made publication safe through synchronizedMap
public Map<String, Date> lastLogin = Collections.synchronizedMap(new HashMap<>());
```
If the Date values are not modified after they are placed in the Map, then the synchronization in the synchronizedMap 
implementation is sufficient to publish the Date values safely, and no additional synchronization is needed when accessing
them.

#### Mutable Objects
If an object may be modified after construction, safe publication ensures only the visibility of the as-published state.
Synchronization must be used not only to publish a mutable object, but also every time the object is accessed to ensure
visibility of subsequent modifications. To share mutable objects safely, they must be safely published and be either
thread-safe or guarded by a lock.

#### Sharing Objects Safely
Whenever you acquire a reference to an object, you should know what you are allowed to do with it. Do you need to acquire
a lock before using it? Are you allowed to modify its state, or only to read it? Many concurrency errors stem from failing
to understand these “rules of engagement” for a shared object. When you publish an object, you should document how the object
can be accessed.

The most useful policies for using and sharing objects in a concurrent program are:
- **Thread-confined**. A thread-confined object is owned exclusively by and confined to one thread, and can be modifled by its
  owning thread.
- **Shared read-only**. A shared read-only object can be accessed concurrently by multiple threads without additional synchronization,
 but cannot be modified by any thread. Shared read-only objects include immutable and effectively immutable objects.
- **Shared thread-safe**. A thread-safe object performs synchronization internally, so multiple threads can freely access it
  through its public interface without further synchronization.
- **Guarded**. A guarded object can be accessed only with a specific lock held. Guarded objects include those that are encapsulated
 within other thread-safe objects and published objects that are known to be guarded by a specific lock.     
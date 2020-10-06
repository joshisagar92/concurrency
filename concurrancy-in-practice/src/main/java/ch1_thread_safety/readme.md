### What is Thread Safety?
At the heart of any reasonable definition of thread safety is the concept of correctness.
Correctness means that a class conforms to its specification.
_No set of operation performed sequentially or concurrently on an instance of thread safe class can cause
instance to be in invalid state._  

#### A Stateless Servlet
StatelessFactorizer is, like most servlets, stateless: it has no fields and references no fields from other classes.
The transient state for a particular computation exists solely in local variables that are stored on the thread's stack 
and are accessible only to the executing thread.

```java
package ch1_thread_safety;

import javax.servlet.*;
import java.io.IOException;

public class StatelessFactorizer implements Servlet {
    @Override
    public void service(ServletRequest servletRequest, ServletResponse servletResponse) throws ServletException, IOException {
        String  name = servletRequest.getParameter("name");
        String[] split = name.split(",");
        servletResponse.getWriter().println(split);
    }
}
```

One thread accessing a StatelessFactorizer cannot influence the result of another thread accessing the same
StatelessFactorizer; because the two threads do not share state, it is as if they were accessing different instances.
Since the actions of a thread accessing a stateless object cannot affect the correctness of operations in other threads,
stateless objects are thread-safe. Stateless objects are always thread-safe.
The fact that most servlets can be implemented with no state greatly reduces the burden of making servlets thread-safe.
It is only when servlets want to remember things from one request to another that the thread safety requirement becomes
an issue.

### Atomicity
What happens when we add one element of state to what was a stateless object? 

[**code**](Atomicity.java)

Suppose we want to add `count` as state of the Atomic class, which will increase on every request.
Now Automic class is not thread safe as StatelessFactorizer. It seems to work correct in single threaded environment but it is 
suffering from issue of _Lost Update_
++count, may look like a single action because of its compact syntax, it is not atomic, which means that it does not 
execute as a single, indivisible operation. Instead, it is a shorthand for a sequence of three discrete operations:
fetch the current value, add one to it, and write the new value back.
This is an example of a read-modify-write operation, in which the resulting state is derived from the previous state.
what can happen if two threads try to increment a counter simultaneously without synchronization. If the counter is
initially 9, with some unlucky timing each thread could read the value, see that it is 9, add one to it, and each set 
the counter to 10. This is clearly not what is supposed to happen; an increment got lost along the way, and the hit
counter is now permanently off by one. 

#### Race Conditions
A race condition occurs when the correctness of a computation depends on the relative timing or interleaving of multiple
threads by the runtime; in other words, when getting the right answer relies on lucky timing.The most common type of 
race condition is check-then-act, where a potentially stale observation is used to make a decision on what to do next.

- read & act(lazy intialization)
 ```java

if(instance == null){
    return new Instance();  // Poor timing can create race condition
}
``` 
- read , modify & write

```java
count++  // read existing value, update it and then write it.
```

#### Compound Actions
Our example contained a sequence of operations that needed to be atomic, or indivisible, relative to other operations on
the same state. To avoid race conditions, there must be a way to prevent other threads from using a variable while we're
in the middle of modifying it, so we can ensure that other threads can observe or modify the state only before we start 
or after we finish, but not in the middle.

To ensure thread safety, check-then-act operations (like lazy initialization) and read-modify-write operations
(like increment) must always be atomic.

```java
class AtomicOperation implement servlet{
    AtomicInteger count = new AtomicInteger(0);
    public void service(ServletRequest request, ServletResponse response) throws InterruptedException {
        Thread.sleep(2000);
        count.incrementAndGet();
        System.out.println(value);
    }
}
```
The java.util.concurrent.atomic package contains atomic variable classes for effecting atomic state transitions on 
numbers and object references. By replacing the long counter with an AtomicLong, we ensure that all actions that access
the counter state are atomic.Because the state of the servlet is the state of the counter and the counter is thread-safe
, our servlet is once again thread-safe.

### Locking
The definition of thread safety requires that invariants be preserved regardless of timing or interleaving of operations
in multiple threads. One invariant of UnsafeLockingFactorizer is that the product of the factors cached in lastFactors
equal the value cached in lastNumber; our servlet is correct only if this invariant always holds. When multiple variables
participate in an invariant, they are not independent: the value of one constrains the allowed value(s) of the others.
Thus when updating one, you must update the others in the same atomic operation.

[**Code**](Locking.java)

With some unlucky timing, UnsafeCachingFactorizer can violate this invariant. Using atomic references, we cannot update 
both lastNumber and lastFactors simultaneously, even though each call to set is atomic; there is still a window of 
vulnerability when one has been modified and the other has not, and during that time other threads could see that the
invariant does not hold. Similarly, the two values cannot be fetched simultaneously: between the time when thread A
fetches the two values, thread B could have changed them, and again A may observe that the invariant does not hold.

**To preserve state consistency, update related state variables in a single atomic operation.**

#### Intrinsic Locks
Java provides a built-in locking mechanism for enforcing atomicity: the synchronized block.
```java
syncronized(lock){
}
```
Intrinsic locks in Java act as mutexes (or mutual exclusion locks), which means that at most one thread may own the lock.
Since only one thread at a time can execute a block of code guarded by a given lock, the synchronized blocks guarded 
the same lock execute atomically with respect to one another. In the context of concurrency, atomicity means the same 
thing as it does in transactional applications—that a group of statements appear to execute as a single, indivisible
unit. No thread executing a synchronized block can observe another thread to be in the middle of a synchronized block 
guarded by the same lock.

#### Reentrancy
If thread tries to acquire a lock that it already holds the request succeeds. 
Reentrancy means that locks are acquired on a per-thread rather than per-invocation basis
Reentrancy is implemented by associating each lock --> (acquisition count & owning thread).

Before acquiring lock,

Lock --> (0,No Thread) --> Lock is considered as unheld.

After acquiring unheld lock

Lock --> (1,Thread-1)

If Same thread acquire lock again then count is incremented.

Lock --> (2,Thread-1)

When thread exit synchronized block count decremented and when count is 0, the lock is released.

Reentrancy facilitates encapsulation of locking behavior, and thus simplifies the development of object-oriented concurrent code. 
Without reentrant locks, the very natural-looking code , in which a subclass overrides a synchronized method and then 
calls the superclass method, would deadlock.
Because the doSomething methods in Widget and LoggingWidget are both synchronized, each tries to acquire the lock on the
Widget before proceeding. But if intrinsic locks were not reentrant, the call to super.doSomething would never be able 
to acquire the lock because it would be considered already held, and the thread would permanently stall waiting for a 
lock it can never acquire. Reentrancy saves us from deadlock in situations like this.

```java
class Widget{
    public synchronized void doSomething(){

    }
}

class LoggingWidget extends Widget{
    @Override
    public synchronized void doSomething() {
        super.doSomething();
    }
}
``` 
### Guarding State with Locks
Because locks enable serialized access to the code paths they guard, we can use them to construct protocols for 
guaranteeing exclusive access to shared state. Following these protocols consistently can ensure state consistency.
serializing access means that threads take turns accessing the object exclusively, rather than doing so concurrently.

Compound actions on shared state, such as incrementing a hit counter (read-modify-write) or lazy initialization 
(check-then-act), must be made atomic to avoid race conditions. Holding a lock for the entire duration of a compound 
action can make that compound action atomic. However, just wrapping the compound action with a synchronized block is not
sufficient; if synchronization is used to coordinate access to a variable, it is needed everywhere that variable is accessed.
Further, when using locks to coordinate access to a variable, the same lock must be used wherever that variable is accessed.

It is a common mistake to assume that synchronization needs to be used only when writing to shared variables;
this is simply not true.

For each mutable state variable that may be accessed by more than one thread, all accesses to that variable must be 
performed with the same lock held. In this case, we say that the variable is guarded by that lock.

Acquiring the lock associated with an object does not prevent other threads from accessing that object—the only thing 
that acquiring a lock prevents any other thread from doing is acquiring that same lock.

A common locking convention is to encapsulate all mutable state within an object and to protect it from concurrent 
access by synchronizing any code path that accesses mutable state using the object's intrinsic lock. This pattern is used
by many thread-safe classes, such as Vector and other synchronized collection classes. 

Not all data needs to be guarded by locks—only mutable data that will be accessed from multiple threads.

For every invariant that involves more than one variable, all the variables involved in that invariant must be guarded 
by the same lock.

### Liveness and Performance
The synchronization policy for SynchronizedFactorizer is to guard each state variable with the servlet object's 
intrinsic lock, and that policy was implemented by synchronizing the entirety of the service method. This simple, 
coarse-grained approach restored safety, but at a high price.
Complete provides a balance between simplicity (synchronizing the entire method) and
concurrency (synchronizing the shortest possible code paths). Acquiring and releasing a lock has some overhead, so it is
undesirable to break down synchronized blocks too far (such as factoring ++hits into its own synchronized block), even 
if this would not compromise atomicity.
Complete.java holds the lock when accessing state variables and for the duration of compound actions, but releases it
before executing the potentially long-running factorization operation. This preserves thread safety without unduly
affecting concurrency; the code paths in each of the synchronized blocks are “short enough”

[**code**](Complete.java)


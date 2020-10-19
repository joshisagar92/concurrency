#### IMPLICIT COUPLINGS BETWEEN TASKS AND EXECUTION POLICIES
Thread pools work best when tasks are homogeneous and independent. Mixing long-running and short-running tasks risks “clogging” the pool
unless it is very large; submitting tasks that depend on other tasks risks deadlock unless the pool is unbounded. Fortunately, requests
in typical network-based server applications—web servers, mail servers, file servers—usually meet these guidelines.

#### Thread Starvation Deadlock
If tasks that depend on other tasks execute in a thread pool, they can deadlock. In a single-threaded executor, a task that submits another
task to the same executor and waits for its result will always deadlock. The second task sits on the work queue until the first task completes,
but the first will not complete because it is waiting for the result of the second task. The same thing can happen in larger thread pools if
all threads are executing tasks that are blocked waiting for other tasks still on the work queue. This is called thread starvation deadlock,
and can occur whenever a pool task initiates an unbounded blocking wait for some resource or condition that can succeed only through the
action of another pool task, such as waiting for the return value or side effect of another task, unless you can guarantee that the pool
is large enough.

Whenever you submit to an Executor tasks that are not independent, be aware of the possibility of thread starvation deadlock, and document
any pool sizing or configuration constraints in the code or configuration file where the Executor is configured.

In addition to any explicit bounds on the size of a thread pool, there may also be implicit limits because of constraints on other resources.
If your application uses a JDBC connection pool with ten connections and each task needs a database connection, it is as if your thread pool
only has ten threads because tasks in excess of ten will block waiting for a connection.

#### Long-running Tasks
Thread pools can have responsiveness problems if tasks can block for extended periods of time, even if deadlock is not a possibility.
A thread pool can become clogged with long-running tasks, increasing the service time even for short tasks. If the pool size is too small 
relative to the expected steady-state number of longrunning tasks, eventually all the pool threads will be running long-running tasks and 
responsiveness will suffer.

One technique that can mitigate the ill effects of long-running tasks is for tasks to use timed resource waits instead of unbounded waits.
Most blocking methods in the plaform libraries come in both untimed and timed versions, such as Thread.join, BlockingQueue.put,
CountDownLatch.await, and Selector.select. If the wait times out, you can mark the task as failed and abort it or requeue it for execution
later. 

###  SIZING THREAD POOLS

For compute-intensive tasks, an Ncpu-processor system usually achieves optimum utilization with a thread pool of Ncpu +1 threads.
(Even compute-intensive threads occasionally take a page fault or pause for some other reason, so an “extra” runnable thread prevents CPU
cycles from going unused when this happens.) For tasks that also include I/O or other blocking operations, you want a larger pool,
since not all of the threads will be schedulable at all times. In order to size the pool properly, you must estimate the ratio of waiting 
time to compute time for your tasks; this estimate need not be precise and can be obtained through pro-filing or instrumentation. 
Alternatively, the size of the thread pool can be tuned by running the application using several different pool sizes under a benchmark load
and observing the level of CPU utilization.

int N_CPUS = Runtime.getRuntime().availableProcessors();

N_thread = n_cpu + u_cpu + (1 + w/c)

n_cpu = no of CPU
u_cpu = target cpu utilization
W/C   = ratio of wait time to compute time

Of course, CPU cycles are not the only resource you might want to manage using thread pools. Other resources that can contribute to sizing
constraints are memory, file handles, socket handles, and database connections. Calculating pool size constraints for these types of
resources is easier: just add up how much of that resource each task requires and divide that into the total quantity available.
The result will be an upper bound on the pool size.

When tasks require a pooled resource such as database connections, thread pool size and resource pool size affect each other. If each task
requires a connection, the effective size of the thread pool is limited by the connection pool size. Similarly, when the only consumers of
connections are pool tasks, the effective size of the connection pool is limited by the thread pool size.


### CONFIGURING THREADPOOLEXECUTOR
ThreadPoolExecutor is a flexible, robust pool implementation that allows a variety of customizations.

#### Thread Creation and Teardown
**core size** is the target size; the implementation attempts to maintain the pool at this size even when there are no tasks to execute,
and will not create more threads than this unless the work queue is full.

The **maximum pool size** is the upper bound on how many pool threads can be active at once. 

A thread that has been idle for longer than the keep-alive time becomes a candidate for reaping and can be terminated if the current pool size
exceeds the core size.

If the pool is already at the core size, ThreadPoolExecutor creates a new thread only if the work queue is full. So tasks submitted to a
thread pool with a work queue that has any capacity and a core size of zero will not execute until the queue fills up, which is usually not
what is desired. In Java 6, allowCoreThreadTimeOut allows you to request that all pool threads be able to time out; enable this feature with
a core size of zero if you want a bounded thread pool with a bounded work queue but still have all the threads torn down when there is no
work to do.
By tuning the core pool size and keep-alive times, you can encourage the pool to reclaim resources used by otherwise idle threads, making
them available for more useful work.

#### Managing Queued Tasks
If the arrival rate for new requests exceeds the rate at which they can be handled, requests will still queue up. With a thread pool,
they wait in a queue of Runnables managed by the Executor instead of queueing up as threads contending for the CPU. Representing a waiting
task with a Runnable and a list node is certainly a lot cheaper than with a thread, but the risk of resource exhaustion still remains if
clients can throw requests at the server faster than it can handle them.

Requests often arrive in bursts even when the average request rate is fairly stable. Queues can help smooth out transient bursts of tasks,
but if tasks continue to arrive too quickly you will eventually have to throttle the arrival rate to avoid running out of memory
Even before you run out of memory, response time will get progressively worse as the task queue grows.

The default for newFixedThreadPool and newSingleThreadExecutor is to use an unbounded LinkedBlockingQueue. Tasks will queue up if all
worker threads are busy, but the queue could grow without bound if the tasks keep arriving faster than they can be executed.

Bounded queues help prevent resource exhaustion but introduce the question of what to do with new tasks when the queue is full.
A large queue coupled with a small pool can help reduce memory usage, CPU usage, and context switching, at the cost of potentially
constraining throughput.

For very large or unbounded pools, you can also bypass queueing entirely and instead hand off tasks directly from producers to worker threads
using a SynchronousQueue. A SynchronousQueue is not really a queue at all, but a mechanism for managing handoffs between threads.
In order to put an element on a SynchronousQueue, another thread must already be waiting to accept the handoff. If no thread is waiting but
the current pool size is less than the maximum, Thread-PoolExecutor creates a new thread; otherwise the task is rejected according to the
saturation policy. Using a direct handoff is more efficient because the task can be handed right to the thread that will execute it,
rather than first placing it on a queue and then having the worker thread fetch it from the queue. SynchronousQueue is a practical choice
only if the pool is unbounded or if rejecting excess tasks is acceptable. The newCachedThreadPool factory uses a SynchronousQueue.

Using a FIFO queue like LinkedBlockingQueue or ArrayBlockingQueue causes tasks to be started in the order in which they arrived.
For more control over task execution order, you can use a PriorityBlockingQueue, which orders tasks according to priority. Priority can be
defined by natural order (if tasks implement Comparable) or by a Comparator.

The newCachedThreadPool factory is a good default choice for an Executor, providing better queuing performance than a fixed thread pool.
A fixed size thread pool is a good choice when you need to limit the number of concurrent tasks for resource-management purposes,
as in a server application that accepts requests from network clients and would otherwise be vulnerable to overload.

Bounding either the thread pool or the work queue is suitable only when tasks are independent. With tasks that depend on other tasks, 
bounded thread pools or queues can cause thread starvation deadlock; instead, use an unbounded pool configuration like newCachedThreadPool

#### Saturation Policies
When a bounded work queue fills up, the saturation policy comes into play. The saturation policy for a ThreadPoolExecutor can be modified
by calling setRejectedExecutionHandler.

Several implementations of RejectedExecutionHandler are provided, each implementing a different saturation policy
- The default policy, abort, causes execute to throw the unchecked Rejected-ExecutionException; the caller can catch this exception and 
  implement its own overflow handling as it sees fit.
- The discard policy silently discards the newly submitted task if it cannot be queued for execution;
- the discard-oldest policy discards the task that would otherwise be executed next and tries to resubmit the new task.
  (If the work queue is a priority queue, this discards the highest-priority element, so the combination of a discard-oldest
  saturation policy and a priority queue is not a good one.)
  
The caller-runs policy implements a form of throttling that neither discards tasks nor throws an exception, but instead tries to slow down
the flow of new tasks by pushing some of the work back to the caller. It executes the newly submitted task not in a pool thread, but in the
thread that calls execute. If we modified our WebServer example to use a bounded queue and the caller-runs policy, after all the pool
threads were occupied and the work queue filled up the next task would be executed in the main thread during the call to execute. Since this
would probably take some time, the main thread cannot submit any more tasks for at least a little while, giving the worker threads some time
to catch up on the backlog. The main thread would also not be calling accept during this time, so incoming requests will queue up in the TCP
layer instead of in the application. If the overload persisted, eventually the TCP layer would decide it has queued enough connection requests
and begin discarding connection requests as well. As the server becomes overloaded, the overload is gradually pushed outward—from the pool
threads to the work queue to the application to the TCP layer, and eventually to the client—enabling more graceful degradation under load.

There is no predefined saturation policy to make execute block when the work queue is full. However, the same effect can be accomplished by
using a Semaphore to bound the task injection rate,

In such an approach, use an unbounded queue (there's no reason to bound both the queue size and the injection rate) and set the bound on the
semaphore to be equal to the pool size plus the number of queued tasks you want to allow, since the semaphore is bounding the number of tasks
both currently executing and awaiting execution.

####  Thread Factories
Whenever a thread pool needs to create a thread, it does so through a thread factory.
The default thread factory creates a new, nondaemon thread with no special configuration. Specifying a thread factory allows you to customize
the configuration of pool threads. ThreadFactory has a single method, newThread, that is called whenever a thread pool needs to create a new
thread.

There are a number of reasons to use a custom thread factory. 
- You might want to specify an UncaughtExceptionHandler for pool threads, or instantiate an instance of a custom Thread class, such as one
  that performs debug logging.
- maybe you just want to give pool threads more meaningful names to simplify interpreting thread dumps and error logs.     
         
 






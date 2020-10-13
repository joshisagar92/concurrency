 ### EXECUTING TASKS IN THREADS
 Ideally, tasks are independent activities: work that doesn't depend on the state, result, or side effects of other tasks.
 SingleThreadedWebServer is simple and theoretically correct, but would perform poorly in production because it can handle only one request 
 at a time.
 In a single-threaded server, blocking not only delays completing the current request, but prevents pending requests from being processed at
 all. If one request blocks for an unusually long time, users might think the server is unavailable because it appears unresponsive.
 At the same time, resource utilization is poor, since the CPU sits idle while the single thread waits for its I/O to complete.
 
 #### Explicitly Creating Threads for Tasks
 ThreadPerTaskWebServer is similar in structure to the single-threaded version—the main thread still alternates between accepting an incoming
 connection and dispatching the request. The difference is that for each connection, the main loop creates a new thread to process the 
 request instead of processing it within the main thread. This has three main consequences:
 
 - Task processing is offloaded from the main thread, enabling the main loop to resume waiting for the next incoming connection more quickly.
  This enables new connections to be accepted before previous requests complete, improving responsiveness.
 - Tasks can be processed in parallel, enabling multiple requests to be serviced simultaneously. This may improve throughput if there are 
   multiple processors, or if tasks need to block for any reason such as I/O completion, lock acquisition, or resource availability.
 - Task-handling code must be thread-safe, because it may be invoked concurrently for multiple tasks.
 
 #### Disadvantages of Unbounded Thread Creation
 For production use, however, the thread-per-task approach has some practical drawbacks, especially when a large number of threads may be
 created:
 
- Thread lifecycle overhead. Thread creation and teardown are not free.If requests are frequent and lightweight, as in most server 
   applications, creating a new thread for each request can consume significant computing resources.
- Resource consumption. Active threads consume system resources, especially memory. When there are more runnable threads than available
  processors, threads sit idle. Having many idle threads can tie up a lot of memory, putting pressure on the garbage collector, and having
  many threads competing for the CPUs can impose other performance costs as well. If you have enough threads to keep all the CPUs busy,
  creating more threads won't help and may even hurt.
- There is a limit on how many threads can be created.When you hit this limit, the most likely result is an OutOfMemoryError. Trying to 
  recover from such an error is very risky; it is far easier to structure your program to avoid hitting this limit.
  
Up to a certain point, more threads can improve throughput, but beyond that point creating more threads just slows down your application,
and creating one thread too many can cause your entire application to crash horribly. The way to stay out of danger is to place some bound on
how many threads your application creates, and to test your application thoroughly to ensure that, even when this bound is reached,
it does not run out of resources.

### THE EXECUTOR FRAMEWORK
We saw how to use bounded queues to prevent an overloaded application from running out of memory. Thread pools offer the same benefit for 
thread management, and java.util.concurrent provides a flexible thread pool implementation as part of the Executor framework. The primary 
abstraction for task execution in the Java class libraries is not Thread, but Executor,

It provides a standard means of decoupling task submission from task execution, describing tasks with Runnable.The Executor implementations
also provide lifecycle support and hooks for adding statistics gathering, application management, and monitoring.
Using an Executor is usually the easiest path to implementing a producer-consumer design in your application.

#### Example: Web Server Using Executor
The value of decoupling submission from execution is that it lets you easily specify, and subsequently change without great difficulty,
the execution policy for a given class of tasks.

-In what thread will tasks be executed?
-In what order should tasks be executed (FIFO, LIFO, priority order)?
-How many tasks may execute concurrently?
-How many tasks may be queued pending execution?
-If a task has to be rejected because the system is overloaded, which task should be selected as the victim, and how should the application be notified?
-What actions should be taken before or after executing a task?

Separating the specification of execution policy from task submission makes it practical to select an execution policy at deployment time that is
matched to the available hardware.

#### Thread Pools
Benifits,
- Reusing an existing thread instead of creating a new one amortizes thread creation and teardown costs over multiple requests.
- By properly tuning the size of the thread pool, you can have enough threads to keep the processors busy while not having so many that your
  application runs out of memory or thrashes due to competition among threads for resources.
  
**newFixedThreadPool** -   A fixed-size thread pool creates threads as tasks are submitted, up to the maximum pool size, and then attempts to
 keep the pool size constant (adding new threads if a thread dies due to an unexpected Exception).

**newCachedThreadPool** - A cached thread pool has more flexibility to reap idle threads when the current size of the pool exceeds the demand
 for processing, and to add new threads when demand increases, but places no bounds on the size of the pool.         
 
**newSingleThreadExecutor** -  A single-threaded executor creates a single worker thread to process tasks, replacing it if it dies unexpectedly.
Tasks are guaranteed to be processed sequentially according to the order imposed by the task queue

**newScheduledThreadPool** - A fixed-size thread pool that supports delayed and periodic task execution, similar to Timer. 

Switching from a thread-per-task policy to a pool-based policy has a big effect on application stability: the web server will no longer fail under
heavy load.And using an Executor opens the door to all sorts of additional opportunities for tuning, management, monitoring, logging, error reporting,
and other possibilities that would have been far more difficult to add without a task execution framework.

#### Executor Lifecycle
We've seen how to create an Executor but not how to shut one down. An Executor implementation is likely to create threads for processing tasks. But the
JVM can't exit until all the (nondaemon) threads have terminated, so failing to shut down an Executor could prevent the JVM from exiting.

The lifecycle implied by ExecutorService has three states—running, shutting down, and terminated. ExecutorServices are initially created in
the running state. The shutdown method initiates a graceful shutdown: no new tasks are accepted but previously submitted tasks are allowed
to complete—including those that have not yet begun execution. The shutdownNow method initiates an abrupt shutdown: it attempts to cancel
outstanding tasks and does not start any tasks that are queued but not begun.

Once all tasks have completed, the ExecutorService transitions to the terminated state. You can wait for an ExecutorService to reach the
terminated state with awaitTermination, or poll for whether it has yet terminated with isTerminated. It is common to follow shutdown
immediately by awaitTermination, creating the effect of synchronously shutting down the ExecutorService. 

####  Delayed and Periodic Tasks
The Timer facility manages the execution of deferred (“run this task in 100 ms”) and periodic (“run this task every 10 ms”) tasks. However,
Timer has some drawbacks, and ScheduledThreadPoolExecutor should be thought of as its replacement.

A Timer creates only a single thread for executing timer tasks. If a timer task takes too long to run, the timing accuracy of other 
TimerTasks can suffer. If a recurring TimerTask is scheduled to run every 10 ms and another Timer-Task takes 40 ms to run, the recurring task
either (depending on whether it was scheduled at fixed rate or fixed delay) gets called four times in rapid succession after the long-running
task completes, or “misses” four invocations completely. Scheduled thread pools address this limitation by letting you provide multiple threads
for executing deferred and periodic tasks.

Another problem with Timer is that it behaves poorly if a TimerTask throws an unchecked exception. The Timer thread doesn't catch the exception,
so an unchecked exception thrown from a TimerTask terminates the timer thread. Timer also doesn't resurrect the thread in this situation;
instead, it erroneously assumes the entire Timer was cancelled. In this case, TimerTasks that are already scheduled but not yet executed are
never run, and new tasks cannot be scheduled.(thread leakage)

OutOfTime illustrates how a Timer can become confused in this manner and, as confusion loves company, how the Timer shares its confusion with
the next hapless caller that tries to submit a TimerTask. You might expect the program to run for six seconds and exit, but what actually
happens is that it terminates after one second with an IllegalStateException whose message text is “Timer already cancelled”.
ScheduledThreadPoolExecutor deals properly with ill-behaved tasks; there is little reason to use Timer in Java 5.0 or later.

```java
public class OutOfTime {
    public static void main(String[] args) throws InterruptedException {
        Timer timer = new Timer();
        timer.schedule(new ThrowTask(),1);
        SECONDS.sleep(1);
        timer.schedule(new ThrowTask(),1);
        SECONDS.sleep(5);
    }
    private static class ThrowTask extends TimerTask {
        @Override
        public void run() {
            throw new RuntimeException();
        }
    }
}
```
### FINDING EXPLOITABLE PARALLELISM
In most server applications, there is an obvious task boundary: a single client request. But sometimes good task boundaries are not quite so
obvious, as in many desktop applications. There may also be exploitable parallelism within a single client request in server applications,
as is sometimes the case in database servers. 

#### Example: Sequential Page Renderer
The simplest approach is to process the HTML document sequentially. As text markup is encountered, render it into the image buffer;
as image references are encountered, fetch the image over the network and draw it into the image buffer as well. This is easy to implement
and requires touching each element of the input only once (it doesn't even require buffering the document), but is likely to annoy the user,
who may have to wait a long time before all the text is rendered.

A less annoying but still sequential approach involves rendering the text elements first, leaving rectangular placeholders for the images,
and after completing the initial pass on the document, going back and downloading the images and drawing them into the associated placeholder.

#### Result-bearing Tasks: Callable and Future
The Executor framework uses Runnable as its basic task representation. Runnable is a fairly limiting abstraction; run cannot return a value
or throw checked exceptions, although it can have side effects such as writing to a log file or placing a result in a shared data structure.

Many tasks are effectively deferred computations—executing a database query, fetching a resource over the network, or computing a 
complicated function. For these types of tasks, Callable is a better abstraction: it expects that the main entry point, call, will return 
a value and anticipates that it might throw an exception. Executors includes several utility methods for wrapping other types of tasks,
including Runnable and java.security.PrivilegedAction, with a Callable.

Runnable and Callable describe abstract computational tasks. Tasks are usually finite: they have a clear starting point and they eventually
terminate. The lifecycle of a task executed by an Executor has four phases: created, submitted, started, and completed. Since tasks can
take a long time to run, we also want to be able to cancel a task. In the Executor framework, tasks that have been submitted but not yet 
started can always be cancelled, and tasks that have started can sometimes be cancelled if they are responsive to interruption. Cancelling 
a task that has already completed has no effect.

Future represents the lifecycle of a task and provides methods to test whether the task has completed or been cancelled, retrieve its result,
and cancel the task.Implicit in the specification of Future is that task lifecycle can only move forwards, not backwards—just like the ExecutorService
lifecycle. Once a task is completed, it stays in that state forever.

The behavior of get varies depending on the task state (not yet started, running, completed). It returns immediately or throws an Exception
if the task has already completed, but if not it blocks until the task completes. If the task completes by throwing an exception, get 
rethrows it wrapped in an ExecutionException; if it was cancelled, get throws CancellationException. If get throws ExecutionException, 
the underlying exception can be retrieved with getCause

There are several ways to create a Future to describe a task. The submit methods in ExecutorService all return a Future, so that you can 
submit a Runnable or a Callable to an executor and get back a Future that can be used to retrieve the result or cancel the task. You can also
explicitly instantiate a FutureTask for a given Runnable or Callable.

Submitting a Runnable or Callable to an Executor constitutes a safe publication of the Runnable or Callable from the submitting
thread to the thread that will eventually execute the task. Similarly, setting the result value for a Future constitutes a safe publication
of the result from the thread in which it was computed to any thread that retrieves it via get. 
 
#### Page Renderer with Future
As a first step towards making the page renderer more concurrent, let's divide it into two tasks, one that renders the text and one that
downloads all the images. (Because one task is largely CPU-bound and the other is largely I/O-bound, this approach may yield improvements
even on single-CPU systems.)

we create a Callable to download all the images, and submit it to an ExecutorService. This returns a Future describing the task's execution;
when the main task gets to the point where it needs the images, it waits for the result by calling Future.get. If we're lucky, the results
will already be ready by the time we ask; otherwise, at least we got a head start on downloading the images.

#### Limitations of Parallelizing Heterogeneous Tasks
Two people can divide the work of cleaning the dinner dishes fairly effectively: one person washes while the other dries. However, assigning
a different type of task to each worker does not scale well; if several more people show up, it is not obvious how they can help without
getting in the way or significantly restructuring the division of labor. Without finding finer-grained parallelism among similar tasks,
this approach will yield diminishing returns.

A further problem with dividing heterogeneous tasks among multiple workers is that the tasks may have disparate sizes. If you divide tasks A
and B between two workers but A takes ten times as long as B, you've only speeded up the total process by 9%. Finally, dividing a task among
multiple workers always involves some amount of coordination overhead; for the division to be worthwhile, this overhead must be more than
compensated by productivity improvements due to parallelism.

FutureRenderer uses two tasks: one for rendering text and one for downloading the images. If rendering the text is much faster than
downloading the images, as is entirely possible, the resulting performance is not much different from the sequential version, but the code
is a lot more complicated. And the best we can do with two threads is speed things up by a factor of two. Thus, trying to increase 
concurrency by parallelizing heterogeneous activities can be a lot of work, and there is a limit to how much additional concurrency you 
can get out of it.

The real performance payoff of dividing a program's workload into tasks comes when there are a large number of independent, homogeneous 
tasks that can be processed concurrently.

#### CompletionService: Executor Meets BlockingQueue
If you have a batch of computations to submit to an Executor and you want to retrieve their results as they become available, you could
retain the Future associated with each task and repeatedly poll for completion by calling get with a timeout of zero. This is possible,
but tedious. Fortunately there is a better way: a completion service.

CompletionService combines the functionality of an Executor and a BlockingQueue. You can submit Callable tasks to it for execution and use
the queuelike methods take and poll to retrieve completed results, packaged as Futures, as they become available.

Multiple ExecutorCompletionServices can share a single Executor, so it is perfectly sensible to create an ExecutorCompletionService that
is private to a particular computation while sharing a common Executor. When used in this way, a CompletionService acts as a handle for a
batch of computations in much the same way that a Future acts as a handle for a single computation. By remembering how many tasks were submitted
to the CompletionService and counting how many completed results are retrieved, you can know when all the results for a given batch have been
retrieved, even if you use a shared Executor.

#### Placing Time Limits on Tasks
The primary challenge in executing tasks within a time budget is making sure that you don't wait longer than the time budget to get an answer
or find out that one is not forthcoming. The timed version of Future.get supports this requirement: it returns as soon as the result is ready,
but throws TimeoutException if the result is not ready within the timeout period.

A secondary problem when using timed tasks is to stop them when they run out of time, so they do not waste computing resources by continuing
to compute a result that will not be used. This can be accomplished by having the task strictly manage its own time budget and abort if it
runs out of time, or by cancelling the task if the timeout expires. Again, Future can help; if a timed get completes with a TimeoutException,
you can cancel the task through the Future.


  






 
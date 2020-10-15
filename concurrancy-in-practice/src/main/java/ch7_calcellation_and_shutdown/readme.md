Getting tasks and threads to stop safely, quickly, and reliably is not always easy. Java does not provide any mechanism for safely forcing
a thread to stop what it is doing.Instead, it provides interruption, a cooperative mechanism that lets one thread ask another to stop what
it is doing.

The cooperative approach is required because we rarely want a task, thread, or service to stop immediately, since that could leave shared data
structures in an inconsistent state. Instead, tasks and services can be coded so that, when requested, they clean up any work currently in 
progress and then terminate.

### TASK CANCELLATION
There are a number of reasons why you might want to cancel an activity:
- User-requested cancellation.
- Time-limited activities. 
- Application events. (When one task finds a solution, all other tasks still searching are cancelled.)
- Errors.
- Shutdown.

There is no safe way to preemptively stop a thread in Java, and therefore no safe way to preemptively stop a task. There are only cooperative
mechanisms, by which the task and the code requesting cancellation follow an agreed-upon protocol.

One such cooperative mechanism is setting a “cancellation requested” flag that the task checks periodically

A task that wants to be cancellable must have a cancellation policy that specifies the “how”, “when”, and “what” of cancellation—how other 
code can request cancellation, when the task checks whether cancellation has been requested, and what actions the task takes in response to a
cancellation request.

[***PrimeGenerator***](PrimeGenerator.java) 

PrimeGenerator uses a simple cancellation policy: client code requests cancellation by calling cancel, PrimeGenerator checks for cancellation
once per prime found and exits when it detects cancellation has been requested.

#### Interruption
The cancellation mechanism in PrimeGenerator will eventually cause the primeseeking task to exit, but it might take a while. If, however,
a task that uses this approach calls a blocking method such as BlockingQueue.put, we could have a more serious problem—the task might never 
check the cancellation flag and therefore might never terminate.

[***BlockingPrimeGenerator***](BlockingPrimeGenerator.java) illustrates this problem.

Thread interruption is a cooperative mechanism for a thread to signal another thread that it should, at its convenience and if it feels like
it, stop what it is doing and do something else.

Each thread has a boolean interrupted status; interrupting a thread sets its interrupted status to true. Thread contains methods for interrupting
a thread and querying the interrupted status of a thread, as shown in Listing 7.4. The interrupt method interrupts the target thread, and 
isInterrupted returns the interrupted status of the target thread. The poorly named static interrupted method clears the interrupted status
of the current thread and returns its previous value; this is the only way to clear the interrupted status.

Blocking library methods like Thread.sleep and Object.wait try to detect when a thread has been interrupted and return early. They respond to
interruption by clearing the interrupted status and throwing InterruptedException, indicating that the blocking operation completed early due
to interruption. The JVM makes no guarantees on how quickly a blocking method will detect interruption, but in practice this happens reasonably
quickly.

There is nothing in the API or language specification that ties interruption to any specific cancellation semantics, but in practice, using 
interruption for anything but cancellation is fragile and difficult to sustain in larger applications.

If a thread is interrupted when it is not blocked, its interrupted status is set, and it is up to the activity being cancelled to poll the 
interrupted status to detect interruption. In this way interruption is “sticky”—if it doesn't trigger an InterruptedException, evidence of 
interruption persists until someone deliberately clears the interrupted status.

A good way to think about interruption is that it does not actually interrupt a running thread; it just requests that the thread interrupt 
itself at the next convenient opportunity. (These opportunities are called cancellation points.) Some methods, such as wait, sleep, and join,
take such requests seriously, throwing an exception when they receive an interrupt request or encounter an already set interrupt status upon
entry. Well behaved methods may totally ignore such requests so long as they leave the interruption request in place so that calling code
can do something with it. Poorly behaved methods swallow the interrupt request, thus denying code further up the call stack the opportunity
to act on it.

The static interrupted method should be used with caution, because it clears the current thread's interrupted status. If you call interrupted
and it returns true, unless you are planning to swallow the interruption, you should do something with it—either throw InterruptedException
or restore the interrupted status by calling interrupt again

[***BlockingPrimeGenerator***](BlockingPrimeGenerator.java) can be easily fixed (and simplified) by using interruption instead of a boolean flag to request cancellation, as shown
. There are two points in each loop iteration where interruption may be detected: in the blocking put call, and by explicitly polling the 
interrupted status in the loop header. The explicit test is not strictly necessary here because of the blocking put call, but it makes 
PrimeProducer more responsive to interruption because it checks for interruption before starting the lengthy task of searching for a prime,
rather than after. When calls to interruptible blocking methods are not frequent enough to deliver the desired responsiveness, explicitly 
testing the interrupted status can help

[***InterruptPrimeGenerator***](InterruptPrimeGenerator.java)

#### Interruption Policies
Just as tasks should have a cancellation policy, threads should have an interruption policy. 
- how a thread interprets an interruption request
- what it does (if anything) when one is detected
- what units of work are considered atomic with respect to interruption
- and how quickly it reacts to interruption

It is important to distinguish between how tasks and threads should react to interruption. A single interrupt request may havemore than one
desired recipient—interrupting a worker thread in a thread pool can mean both “cancel the current task” and “shut down the worker thread”.

Tasks do not execute in threads they own; they borrow threads owned by a service such as a thread pool. Code that doesn't own the thread
(for a thread pool, any code outside of the thread pool implementation) should be careful to preserve the interrupted status so that the 
owning code can eventually act on it, even if the “guest” code acts on the interruption as well. 

A task needn't necessarily drop everything when it detects an interruption request—it can choose to postpone it until a more opportune time
by remembering that it was interrupted, finishing the task it was performing, and then throwing InterruptedException or otherwise
indicating interruption. This technique can protect data structures from corruption when an activity is interrupted in the middle of an
update.

A task should not assume anything about the interruption policy of its executing thread unless it is explicitly designed to run within a
service that has a specific interruption policy.

Whether a task interprets interruption as cancellation or takes some other action on interruption, it should take care to preserve the
executing thread's interruption status.

If it is not simply going to propagate InterruptedException to its caller, it should restore the interruption status after catching
InterruptedException: Thread.currentThread().interrupt();

A thread should be interrupted only by its owner; the owner can encapsulate knowledge of the thread's interruption policy in an appropriate
cancellation mechanism such as a shutdown method.

Because each thread has its own interruption policy, you should not interrupt a thread unless you know what interruption means to that
 thread. 

####  Responding to Interruption
When you call an interruptible blocking method such as Thread.sleep or BlockingQueue.put, there are two practical strategies for handling 
InterruptedException:

- Propagate the exception (possibly after some task-specific cleanup), making your method an interruptible blocking method
- Restore the interruption status so that code higher up on the call stack can deal with it.he standard way to do this is to
 restore the interrupted status by calling interrupt again.
 
What you should not do is swallow the InterruptedException by catching it and doing nothing in the catch block, unless your 
code is actually implementing the interruption policy for a thread.

If you don't want to or cannot propagate InterruptedException (perhaps because your task is defined by a Runnable), you need to find another
way to preserve the interruption request. The standard way to do this is to restore the interrupted status by calling interrupt again.
What you should not do is swallow the InterruptedException by catching it and doing nothing in the catch block, unless **your code** is
actually implementing the interruption policy for a thread.

[***InterruptPrimeGenerator***](InterruptPrimeGenerator.java) swallows the interrupt, but does so with the knowledge that the thread is 
about to terminate and that therefore there is no code higher up on the call stack that needs to know about the interruption. Most code does
not know what thread it will run in and so should preserve the interrupted status.

Only code that implements a thread's interruption policy may swallow an interruption request.
General-purpose task and library code should never swallow interruption requests.

As shown below, Activities that do not support cancellation but still call interruptible blocking methods will have to call them in a loop,
retrying when interruption is detected. In this case, they should save the interruption status locally and restore it just before returning
rather than immediately upon catching InterruptedException.

```java
public Task getNextTask(BlockingQueue<Task> queue){
        boolean interrupted = false;

        try {
            while (true){
             try {
                    return queue.take();
                } catch (InterruptedException e) {
                    interrupted = true;
                    //fall through and retry
                }
            }
        } finally {
            if(interrupted){
                Thread.currentThread().interrupt();
            }
        }

    }
```
Setting the interrupted status too early could result in an infinite loop, because most interruptible blocking methods check the interrupted
status on entry and throw InterruptedException immediately if it is set.Interruptible methods usually poll for interruption before blocking or
doing any significant work, so as to be as responsive to interruption as possible.
 
**Cancellation can involve state other than the interruption status; interruption can be used to get the thread's attention, and information
stored elsewhere by the interrupting thread can be used to provide further instructions for the interrupted thread.**

For example, when a worker thread owned by a ThreadPoolExecutor detects interruption, it checks whether the pool is being shut down. If so,
it performs some pool cleanup before terminating; otherwise it may create a new thread to restore the thread pool to the desired size. 

### Example: Timed Run

[***PrimeGenerator***](PrimeGenerator.java)  and interrupts it after a second. While the PrimeGenerator might take somewhat longer than a second to stop, it will
eventually notice the interrupt and stop, allowing the thread to terminate. But another aspect of executing a task is that you want to
find out if the task throws an exception. If PrimeGenerator throws an unchecked exception before the timeout expires, it will probably go
unnoticed, since the prime generator runs in a **separate thread** that does not explicitly handle exceptions.

Following is an attempt at running an arbitrary Runnable for a given amount of time.It runs the task in the calling thread and schedules
a cancellation task to interrupt it after a given time interval. This addresses the problem of unchecked exceptions thrown from the task,
since they can then be caught by the caller of timedRun.

```java
private static ScheduledExecutorService cancService = Executors.newScheduledThreadPool(10);

    public static void timedRun(Runnable r, long timeout, TimeUnit timeUnit){
        final Thread taskThread = Thread.currentThread();
        cancService.schedule(() -> {taskThread.interrupt();},timeout,timeUnit);
        r.run();
    }
```
This is an appealingly simple approach, but it violates the rules: you should know a thread's interruption policy before interrupting it. 
Since timedRun can be called from an arbitrary thread, it cannot know the calling thread's interruption policy.
If the task completes before the timeout, the cancellation task(cancService.schedule()) that interrupts the thread in which timedRun was 
called could go off after timedRun has returned to its caller.We don't know what code will be running when that happens, but the result
won't be good. 

Following code addresses the exception-handling problem of aSecondOfPrimes and the problems with the previous attempt. The thread created to run the task
can have its own execution policy, and even if the task doesn't respond to the interrupt, the timed run method can still return to its caller
. After starting the task thread, timedRun executes a timed join with the newly created thread. After join returns, it checks if an 
exception was thrown from the task and if so, rethrows it in the thread calling timedRun. The saved Throwable is shared between the two
threads, and so is declared volatile to safely publish it from the task thread to the timedRun thread.

This version addresses the problems in the previous examples, but because it relies on a timed join, it shares a deficiency with join: we don't
know if control was returned because the thread exited normally or because the join timed out
```java
private static ScheduledExecutorService cancService = Executors.newScheduledThreadPool(10);

    public static void timedRun(Runnable r, long timeout, TimeUnit timeUnit) throws InterruptedException {
        class RethrowableTask implements Runnable{
            private volatile Throwable t;
            @Override
            public void run() {
                try {
                    r.run();
                } catch (Throwable t) {
                    this.t = t;
                }
            }

            void rethrow(){
                if(t != null){
                    throw new RuntimeException();
                }
            }
        }

        RethrowableTask task = new RethrowableTask();
        final Thread taskThread = new Thread(task);
        taskThread.start();
        cancService.schedule(() -> taskThread.interrupt(),timeout,timeUnit);
        taskThread.join(timeUnit.toMillis(timeout));
        task.rethrow();

    }
```

####  Cancellation Via Future
Future has a cancel method that takes a boolean argument, mayInterruptIfRunning, and returns a value indicating whether the cancellation
attempt was successful.
When mayInterruptIfRunning is true and the task is currently running in some thread, then that thread is interrupted.
Setting this argument to false means “don't run this task if it hasn't started yet”, and should be used for tasks that are not designed
to handle interruption.
Since you shouldn't interrupt a thread unless you know its interruption policy, when is it OK to call cancel with an argument of true? 
The task execution threads created by the standard Executor implementations implement an interruption policy that lets tasks be cancelled
using interruption, so it is safe to set mayInterruptIfRunning when cancelling tasks through their Futures when they are running in a
standard Executor. You should not interrupt a pool thread directly when attempting to cancel a task, because you won't know what task is
running when the interrupt request is delivered—do this only through the task's Future. This is yet another reason to code tasks to treat
interruption as a cancellation request: then they can be cancelled through their Futures

 If get terminates with a TimeoutException, the task is cancelled via its Future. (To simplify coding, this version calls Future.cancel 
 unconditionally in a finally block, taking advantage of the fact that cancelling a completed task has no effect.) If the underlying 
 computation throws an exception prior to cancellation, it is rethrown from timedRun, which is the most convenient way for the caller to 
 deal with the exception. 

```java
private static ExecutorService exeTask = Executors.newFixedThreadPool(10);

    public static void timedRun(Runnable r, long timeout, TimeUnit timeUnit) throws InterruptedException {
        Future<?> task = exeTask.submit(r);

        try {
            task.get(timeout,timeUnit);
        } catch (ExecutionException e) {
            //Exception thrown in task, rethrow
        } catch (TimeoutException e) {
           //Task will be cancelled here.
        }finally {
            //Harmless if task is completed successfully
            task.cancel(true); //Interrupt if running
        }

    }
```
#### Dealing with Non-interruptible Blocking
Many blocking library methods respond to interruption by returning early and throwing InterruptedException, which makes it easier to build
tasks that are responsive to cancellation.However, not all blocking methods or blocking mechanisms are responsive to interruption; if a 
thread is blocked performing synchronous socket I/O or waiting to acquire an intrinsic lock, interruption has no effect other than setting
the thread's interrupted status.

**Synchronous socket I/O in java.io**. The common form of blocking I/O in server applications is reading or writing to a socket. Unfortunately,
the read and write methods in InputStream and OutputStream are not responsive to interruption, but closing the underlying socket makes any
threads blocked in read or write throw a SocketException.

**Synchronous I/O in java.nio**. Interrupting a thread waiting on an InterruptibleChannel causes it to throw ClosedByInterruptException and close
the channel (and also causes all other threads blocked on the channel to throw ClosedByInterruptException).  

**Asynchronous I/O with Selector**

**Lock acquisition.**If a thread is blocked waiting for an intrinsic lock, there is nothing you can do to stop it short of ensuring that it 
eventually acquires the lock and makes enough progress that you can get its attention some other way. 

Following shows a technique for encapsulating nonstandard cancellation. ReaderThread manages a single socket connection, reading synchronously
from the socket and passing any data received to processBuffer. To facilitate terminating a user connection or shutting down the server,
ReaderThread overrides interrupt to both deliver a standard interrupt and close the underlying socket; thus interrupting a ReaderThread
makes it stop what it is doing whether it is blocked in read or in an interruptible blocking method.

```java
 public class ReaderThread extends Thread{

        private final Socket socket;
        private final InputStream inputStream;

        public ReaderThread(Socket socket) throws IOException {
            this.socket = socket;
            this.inputStream = socket.getInputStream();
        }

        @Override
        public void interrupt() {
            try {
                socket.close();
            } catch (IOException e) {
                //Ignore
            } finally {
                super.interrupt();

            }
        }

        @Override
        public void run() {
            try {
                byte [] buf = new byte[100];
                while (true){

                    int count = inputStream.read(buf);
                    if(count < 0){
                        break;
                    }else if(count > 0){
                        processBuff(buf,count);
                    }
                }
            } catch (IOException e) {
                //Allow thread to exit.
            }
        }

    }
```

####  Encapsulating nonstandard cancellation with newtaskFor
The technique used in ReaderThread to encapsulate nonstandard cancellation can be refined using the newTaskFor hook added to ThreadPoolExecutor
in Java 6. When a Callable is submitted to an ExecutorService, submit returns a Future that can be used to cancel the task. The newTaskFor 
hook is a factory method that creates the Future representing the task. It returns a RunnableFuture, an interface that extends both Future
and Runnable

Customizing the task Future allows you to override Future.cancel. Custom cancellation code can perform logging or gather statistics on 
cancellation, and can also be used to cancel activities that are not responsive to interruption. ReaderThread encapsulates cancellation of
socket-using threads by overriding interrupt; the same can be done for tasks by overriding Future.cancel

```java
interface CancellableTask<T> extends Callable<T>{
        void cancel();
        RunnableFuture<T> newTask();
    }

    public class CancellingExecutor extends ThreadPoolExecutor{

        public CancellingExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
            super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
        }

        protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable){
            if(callable instanceof CancellableTask){
                return ((CancellableTask<T>) callable).newTask();
            }else {
                return super.newTaskFor(callable);
            }
        }
    }

    public abstract class SocketUsingTask<T> implements CancellableTask<T>{

        private Socket socket;

        @Override
        public void cancel() {
            if(socket != null){
                try {
                    socket.close();
                } catch (IOException e) {
                   //Ignore
                }
            }

        }

        @Override
        public RunnableFuture<T> newTask() {
           return new FutureTask<T>(this){
               @Override
               public boolean cancel(boolean mayInterruptIfRunning) {
                   try{
                       SocketUsingTask.this.cancel();
                   }finally {
                        return super.cancel(mayInterruptIfRunning);
                   }
               }
           };
        }
    }
```



 






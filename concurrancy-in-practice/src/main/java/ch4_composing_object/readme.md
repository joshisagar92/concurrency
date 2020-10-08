### DESIGNING A THREAD-SAFE CLASS
While it is possible to write a thread-safe program that stores all its state in public static fields, it is a lot harder
to verify its thread safety or to modify it so that it remains thread-safe than one that uses encapsulation appropriately.
Encapsulation makes it possible to determine that a class is thread-safe without having to examine the entire program.

The design process for a thread-safe class should include these three basic elements:
- Identify the variables that form the object's state;
- Identify the invariants that constrain the state variables;
- Establish a policy for managing concurrent access to the object's state.

An object's state starts with its fields. If they are all of primitive type, the fields comprise the entire state. **Counter**
has only one field, so the value field comprises its entire state. The state of an object with n primitive fields is just
the n-tuple of its field values; the state of a 2D **Point** is its (x, y) value.
If the object has fields that are references to other objects, its state will encompass fields from the referenced objects as well.
For example, the state of a **LinkedList** includes the state of all the link node objects belonging to the list.

The synchronization policy defines how an object coordinates access to its state without violating its invariants or 
postconditions. It specifies what combination of immutability, thread confinement, and locking is used to maintain thread
safety, and which variables are guarded by which locks. To ensure that the class can be analyzed and maintained,
document the synchronization policy.

```java
//annotation will be used as Thread safety policy documentation.
@ThreadSafe
public class Counter {
    @GuardedBy("this")
    private long value = 0;

    public synchronized long  getValue() {
        return value;
    }

    public synchronized long increment() {
        if(value == Long.MAX_VALUE){
            throw  new IllegalStateException("counter overflow");
        }
        return value++;
    }
}
```
#### Gathering Synchronization Requirements
Making a class thread-safe means ensuring that its invariants hold under concurrent access; this requires reasoning about
its state. Objects and variables have a state space: the range of possible states they can take on. The smaller this state
space, the easier it is to reason about. By using final fields wherever practical, you make it simpler to analyze the possible
states an object can be in.
 
Many classes have **invariants** that identify certain **states as valid or invalid**. The value field in Counter is a long.
The state space of a long ranges from Long.MIN_VALUE to Long.MAX_VALUE, but Counter places constraints on value; negative
values are not allowed(as intilaized with 0 and always get incremented).

Similarly, operations may have **postconditions** that identify certain **state transitions as invalid**.
If the current state of a Counter is 17, the only valid next state is 18. When the next state is derived from the current state,
the operation is necessarily a compound action.Not all operations impose state transition constraints; when updating a 
variable that holds the current temperature,its previous state does not affect the computation.

Constraints placed on states or state transitions by invariants and postconditions create additional synchronization or 
encapsulation requirements. If certain states are invalid, then the underlying state variables must be encapsulated,
otherwise client code could put the object into an invalid state. If an operation has invalid state transitions,
it must be made atomic. 

A class can also have invariants that constrain multiple state variables.related variables must be fetched or updated in
a single atomic operation. You cannot update one, release and reacquire the lock, and then update the others, since this
could involve leaving the object in an invalid state when the lock was released. When multiple variables participate in 
an invariant, the lock that guards them must be held for the duration of any operation that accesses the related variables.

You cannot ensure thread safety without understanding an object's invariants and postconditions. Constraints on the valid
values or state transitions for state variables can create atomicity and encapsulation requirements.

#### State-dependent Operations
Class invariants and method postconditions constrain the valid states and state transitions for an object. Some objects 
also have methods with state-based preconditions. For example, you cannot remove an item from an empty queue; a queue must
be in the “nonempty” state before you can remove an element. 

**Operations with state-based preconditions are called state-dependent**

#### State Ownership
Ownership is not embodied explicitly in the language, but is instead an element of class design. If you allocate and populate a HashMap, you
are creating multiple objects: the HashMap object, a number of Map.Entry objects used by the implementation of HashMap, and perhaps other 
internal objects as well. The logical state of a HashMap includes the state of all its Map.Entry and internal objects, even though they are
implemented as separate objects.

For better or worse, garbage collection lets us avoid thinking carefully about ownership. When passing an object to a method in C++,
you have to think fairly carefully about whether you are transferring ownership, engaging in a short-term loan, or envisioning long-term
joint ownership. In Java, all these same ownership models are possible, but the garbage collector reduces the cost of many of the common
errors in reference sharing, enabling less-than-precise thinking about ownership.

It is the owner of a given state variable that gets to decide on the locking protocol used to maintain the integrity of that variable's
state. Ownership implies control, but once you publish a reference to a mutable object, you no longer have exclusive control; at best, you
might have “shared ownership”. A class usually does not own the objects passed to its methods or constructors, unless the method is designed
to explicitly transfer ownership of objects passed in

### INSTANCE CONFINEMENT
Encapsulation simplifies making classes thread-safe by promoting instance confinement, often just called confinement.When an object is 
encapsulated within another object, all code paths that have access to the encapsulated object are known and can be therefore be analyzed 
more easily than if that object were accessible to the entire program. Combining confinement with an appropriate locking discipline can 
ensure that otherwise non-thread-safe objects are used in a thread-safe manner.Confined objects must not escape their intended scope.

PersonSet illustrates how confinement and locking can work together to make a class thread-safe even when its component state
variables are not. The state of PersonSet is managed by a HashSet, which is not thread-safe. But because mySet is private and not allowed to
escape, the HashSet is confined to the PersonSet. The only code paths that can access mySet are addPerson and containsPerson, and each of
these acquires the lock on the PersonSet. All its state is guarded by its intrinsic lock, making PersonSet thread-safe.

```java
class PersonSet {
private final Set<Person> set = new HashSet<>();

public synchronized void add(Person p){
    set.add(p);
}

public synchronized boolean containsPerson(Person p){
    return set.contains(p);
}

}

class Person{

}
```
This example makes no assumptions about the thread-safety of Person, but if it is mutable, additional synchronization will be needed when
accessing a Person retrieved from a PersonSet. The most reliable way to do this would be to make Person thread-safe; less reliable would be
to guard the Person objects with a lock and ensure that all clients follow the protocol of acquiring the appropriate lock before accessing
the Person.

Of course, it is still possible to violate confinement by publishing a supposedly confined object; if an object is intended to be confined
to a specific scope, then letting it escape from that scope is a bug. Confined objects can also escape by publishing other objects such as
iterators or inner class instances that may indirectly publish the confined objects.

Confinement makes it easier to build thread-safe classes because a class that confines its state can be analyzed for thread safety without
having to examine the whole program.

#### The Java Monitor Pattern
Following the principle of instance confinement to its logical conclusion leads you to the Java monitor pattern.[2] An object following the 
Java monitor pattern encapsulates all its mutable state and guards it with the object's own intrinsic lock.

The bytecode instructions for entering and exiting a synchronized block are even called monitorenter and monitorexit, and Java's built-in
(intrinsic) locks are sometimes called monitor locks or monitors.

[***Counter***](Counter.java) above shows a typical example of this pattern. It encapsulates one state variable, value, and
all access to that state variable is through the methods of Counter, which are all synchronized.
The Java monitor pattern is used by many library classes, such as Vector and Hashtable.

The Java monitor pattern is merely a convention; any lock object could be used to guard an object's state so long as it is used consistently.
as shown below.

```java
class PrivateLock{
    private Object lock = new Object();
    Widget widget;
    
    void someMethod(){
        synchronized (lock){
            //do something;
        }
    }
}
```
#### Example: Tracking Fleet Vehicles
[***Code***](MonitorVehicleTracker.java)

### DELEGATING THREAD SAFETY
In CountingFactorizer on page 23, we added an AtomicLong to an otherwise stateless object, and the resulting composite object was still thread-safe.
Since the state of CountingFactorizer is the state of the thread-safe AtomicLong, and since CountingFactorizer imposes no additional validity
constraints on the state of the counter, it is easy to see that CountingFactorizer is thread-safe. We could say that CountingFactorizer
delegates its thread safety responsibilities to the AtomicLong: CountingFactorizer is thread-safe because AtomicLong is.

If count were not final, the thread safety analysis of CountingFactorizer would be more complicated. If CountingFactorizer could modify
count to reference a different AtomicLong, we would then have to ensure that this update was visible to all threads that might access the
count, and that there were no race conditions regarding the value of the count reference. This is another good reason to use final fields
wherever practical.
#### Example: Vehicle Tracker Using Delegation
As a more substantial example of delegation, let's construct a version of the vehicle tracker that delegates to a thread-safe class.
We store the locations in a Map, so we start with a thread-safe Map implementation, ConcurrentHashMap. We also store the location using an
immutable Point class instead of MutablePoint

Point is thread-safe because it is immutable. Immutable values can be freely shared and published, so we no longer need to copy the locations
when returning them.
[***Code***](DelegatingVehicleTracker.java)
If we had used the original MutablePoint class instead of Point, we would be breaking encapsulation by letting getLocations publish a reference to
mutable state that is not thread-safe. Notice that we've changed the behavior of the vehicle tracker class slightly; while the monitor 
version returned a snapshot of the locations, the delegating version returns an unmodifiable but “live” view of the vehicle locations. This 
means that if thread A calls getLocations and thread B later modifies the location of some of the points, those changes are reflected in the
Map returned to thread A. As we remarked earlier, this can be a benefit (more up-to-date data) or a liability (potentially inconsistent view
of the fleet), depending on your requirements.

#### Independent State Variables
The delegation examples so far delegate to a single, thread-safe state variable. We can also delegate thread safety to more than one underlying state
variable as long as those underlying state variables are independent, meaning that the composite class does not impose any invariants
involving the multiple state variables.

VisualComponent in Listing 4.9 is a graphical component that allows clients to register listeners for mouse and keystroke events.
It maintains a list of registered listeners of each type, so that when an event occurs the appropriate listeners can be invoked. But there is
no relationship between the set of mouse listeners and key listeners; the two are independent, and therefore VisualComponent can delegate
its thread safety obligations to two underlying thread-safe lists.

[***Code***](VisualComponent.java)

VisualComponent uses a CopyOnWriteArrayList to store each listener list; this is a thread-safe List implementation particularly suited for 
managing listener lists. Each List is thread-safe, and because there are no constraints coupling the state of one to the state of the other,
VisualComponent can delegate its thread safety responsibilities to the underlying mouseListeners and keyListeners objects.

#### When Delegation Fails

NumberRange is not thread-safe; it does not preserve the invariant that constrains lower and upper. The setLower and setUpper methods
attempt to respect this invariant, but do so poorly. Both setLower and setUpper are check-then-act sequences, but they do not use sufficient
locking to make them atomic. If the number range holds (0, 10), and one thread calls setLower(5) while another thread calls setUpper(4),
with some unlucky timing both will pass the checks in the setters and both modifications will be applied. The result is that the range now
holds (5, 4)—an invalid state. So while the underlying AtomicIntegers are thread-safe, the composite class is not. Because the underlying
state variables lower and upper are not independent, NumberRange cannot simply delegate thread safety to its thread-safe state variables.

If a class has compound actions, as NumberRange does, delegation alone is again not a suitable approach for thread safety. In these cases,
the class must provide its own locking to ensure that compound actions are atomic, unless the entire compound action can also be delegated 
to the underlying state variables.

[***Code***](NumberRange.java)

If a class is composed of multiple independent thread-safe state variables and has no operations that have any invalid state transitions,
then it can delegate thread safety to the underlying state variables.

#### Publishing underlying state variables
When you delegate thread safety to an object's underlying state variables, under what conditions can you publish those variables so that 
other classes can modify them as well? Again, the answer depends on what invariants your class imposes on those variables. While the 
underlying value field in Counter could take on any integer value, Counter constrains it to take on only positive values, and the increment
operation constrains the set of valid next states given any current state. If you were to make the value field public, clients could change
it to an invalid value, so publishing it would render the class incorrect. On the other hand, if a variable represents the current 
temperature or the ID of the last user to log on, then having another class modify this value at any time probably would not violate any 
invariants, so publishing this variable might be acceptable. (It still may not be a good idea, since publishing mutable variables constrains
future development and opportunities for subclassing, but it would not necessarily render the class not thread-safe.)


If a state variable is thread-safe, does not participate in any invariants that constrain its value, and has no prohibited state transitions
for any of its operations, then it can safely be published.

For example, it would be safe to publish mouseListeners or keyListeners in VisualComponent. Because VisualComponent does not impose any 
constraints on the valid states of its listener lists, these fields could be made public or otherwise published without compromising thread
safety.

#### Example: Vehicle Tracker that Publishes Its State
PublishingVehicleTracker derives its thread safety from delegation to an underlying ConcurrentHashMap, but this time the contents of the Map
are thread-safe mutable points rather than immutable ones. The getLocation method returns an unmodifiable copy of the underlying Map. Callers
cannot add or remove vehicles, but could change the location of one of the vehicles by mutating the SafePoint values in the returned Map.
Again, the “live” nature of the Map may be a benefit or a drawback, depending on the requirements. PublishingVehicleTracker is thread-safe,
but would not be so if it imposed any additional constraints on the valid values for vehicle locations. If it needed to be able to “veto”
changes to vehicle locations or to take action when a location changes, the approach taken by PublishingVehicleTracker would not be 
appropriate.

[***Code***](PublishingVehicleTracker.java)

### ADDING FUNCTIONALITY TO EXISTING THREAD-SAFE CLASSES
 - The safest way to add a new atomic operation is to modify the original class to support the desired operation, but this is not always 
 possible because you may not have access to the source code or may not be free to modify it. If you can modify the original class, you need
 to understand the implementation's synchronization policy so that you can enhance it in a manner consistent with its original design. 
 Adding the new method directly to the class means that all the code that implements the synchronization policy for that class is still 
 contained in one source file, facilitating easier comprehension and maintenance.
 
 - Another approach is to extend the class, assuming it was designed for extension.
 Extension is more fragile than adding code directly to a class, because the implementation of the synchronization policy is now distributed
 over multiple, separately maintained source files. If the underlying class were to change its synchronization policy by choosing a different
 lock to guard its state variables, the subclass would subtly and silently break, because it no longer used the right lock to control
 concurrent access to the base class's state.
 
 #### Client-side Locking
 For an ArrayList wrapped with a Collections.synchronizedList wrapper, neither of these approaches—adding a method to the original class or
 extending the class—works because the client code does not even know the class of the List object returned from the synchronized wrapper
 factories.
 A third strategy is to extend the functionality of the class without extending the class itself by placing extension code in a “helper” class.
 Following code shows a failed attempt to create a helper class with an atomic put-if-absent operation for operating on a thread-safe List.
 
 
 ```java
class ListHelper<E>{
    public List<E> list = Collections.synchronizedList(new ArrayList<>());

    public synchronized boolean putIfAbsent(E element){
        if(!list.contains(element)){
            return list.add(element);
        }
        return false;
    }
}
```
 After all, putIfAbsent is synchronized, right? The problem is that it synchronizes on the wrong lock. Whatever lock the List uses to guard
 its state, it sure isn't the lock on the ListHelper. ListHelper provides only the illusion of synchronization; the various list operations,
 while all synchronized, use different locks, which means that putIfAbsent is not atomic relative to other operations on the List. So there
 is no guarantee that another thread won't modify the list while putIfAbsent is executing.
 To make this approach work, we have to use the same lock that the List uses by using client-side locking or external locking. 
 The documentation for Vector and the synchronized wrapper classes states, albeit obliquely, that they support client-side locking, by using
 the intrinsic lock for the Vector or the wrapper collection 

```java
class ListHelper<E>{
    public List<E> list = Collections.synchronizedList(new ArrayList<>());

    public  boolean putIfAbsent(E element){
        synchronized (list){ // Lock should be on list as it list is using intrinsic lock
            if(!list.contains(element)){
                return list.add(element);
            }
            return false;
        }

    }
}
```
If extending a class to add another atomic operation is fragile because it distributes the locking code for a class over multiple classes
in an object hierarchy, client-side locking is even more fragile because it entails putting locking code for class C into classes that are 
totally unrelated to C. Exercise care when using client-side locking on classes that do not commit to their locking strategy.

Client-side locking has a lot in common with class extension—they both couple the behavior of the derived class to the implementation of the
base class. Just as extension violates encapsulation of implementation, client-side locking violates encapsulation of
synchronization policy.

#### Composition
Delegating them to an underlying List instance, and adds an atomic putIfAbsent method.
```java
class ImprovedList<E> implements List<E>{
    public List<E> list;

    public ImprovedList(List<E> list) {
        this.list = list;
    }

    public synchronized boolean putIfAbsent(E element){
            if(!list.contains(element)){
                return list.add(element);
            }
            return false;
        }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }
    
    ....
```
ImprovedList adds an additional level of locking using its own intrinsic lock. It does not care whether the underlying List is thread-safe,
because it provides its own consistent locking that provides thread safety even if the List is not thread-safe or changes its locking
implementation. While the extra layer of synchronization may add some small performance penalty

### DOCUMENTING SYNCHRONIZATION POLICIES
Document a class's thread safety guarantees for its clients; document its synchronization policy for its maintainers.
Each use of synchronized, volatile, or any thread-safe class reflects a synchronization policy defining a strategy for ensuring the integrity
of data in the face of concurrent access. That policy is an element of your program's design, and should be documented. Of course, the best
time to document design decisions is at design time.


  
  
  
   





  


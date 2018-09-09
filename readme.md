AsyncFlows Framework 0.1.0
--------------------------

The framework support easy and modular construction of asynchronous processes from simpler constructs.
The framework is mostly targeted to IO-bound processes and it is not intended for CPU-bound processes.

# Project information
## Licensing

The framework is licensed under copyleft [MIT license](LICENSE.txt). This could change if the project
is migrated to other project groups (Apache, Eclipse, or something else). There is currently no legal
framework to accept project contributions in form of the code. However, bug reporting, experimental
forking, and other feedback is welcome. The project might migrate to some project group to solve this
problem.

Currently project is considered in the Alpha phase and there could be backward incompatible changes
even for minor versions.

## Code Samples

The code samples in this guide are usually taken from unit tests of the framework. So they are directly
executable. If copied in own code, one usually need to resolve some static imports that are used by DSL.

Syntax highlighting for samples is disabled for now because of the bug in IDEA 
[RUBY-18425](https://youtrack.jetbrains.com/issue/RUBY-18425).

## Distribution

The project is currently available only on [github](https://github.com/const/asyncflows). 
There is no builds in maven central.

# Framework Foundations

The concept described in this section are foundations of the framework.
While they are foundation, the user of the framework rarely interacts 
with them directly, so do not assume that code samples here are anything
like what you would see in application. Like with real building,
foundations are mostly stay hidden from the sight. 

## Vats

A Vat is Executor that has the following guarantees:

1. It executes event in order that was sent
2. It executes only one event at each time
3. During execution it is possible to get the current Vat

These guarantees allows avoiding a lot of concurrency issues and organize
asynchronous processes a lot easier.

The concept of the vat is taken from [E programming language](http://www.e-elang.org), from which 
many ideas were borrowed in this framework.

While a vat is handling events, it specifies itself in thread context. So it is available with `Vat.current()`. 
Asynchronous operations in the framework generally inherit `Vat` as execution context, unless the executor 
is specified explicitly.

The is a special cached thread pool with daemon threads that is used for daemon vats `Vats.daemonVat()`.

There are following vats in the core library (there are also some vats in additional libraries):
* `Vat` - abstract class for all vats
* `AWTVat` - vat over AWT event queue
* `BatchedVat` - abstract vat that executes event in batches
* `ExecutorVat` - a vat that runs over some executor. Note, that this vat occupies executor thread only when there
  are events to handle. If there are no events, no threads are occupied. Vat re-scedule itself after fixed batch 
  of events is processed even if there are still events in the queue in order to give other vats of over 
  the same executor a chance to do processing.
* `SingleThreadVatWithIdle` - an abstract vat that occupies one thread and need to periodically poll events 
  from external source (for example NIO events).
* `SingeThreadVat` - a vat that occupies entire thread and can be stopped. This vat is usually used in unit tests
  and to start application on the main thread.

For example, the vat could be used like the following, if more high-level constructs could not be used otherwise.  
```$java
        final Cell<Vat> result = new Cell<>(); // create holder for value
        final SingleThreadVat vat = new SingleThreadVat(null); // create vat
        vat.execute(() -> { // schedule event
            result.setValue(Vat.current()); // save current vat value
            vat.stop(null); // stop vat execution
        });
        assertNull(result.getValue()); // check that it is not executed yet
        vat.runInCurrentThread(); // start vat and execute event
        // vat is stopped
        assertSame(vat, result.getValue()); // get vat value
```  

It is rarely needed to use vat directly. The typical cases are:
* Application setup
* Library or framework code

### Default Vat

When an asynchronous context is needed, but it is not clear whether the current thread has one,
It is possible to use `Vat.defaultVat()` method, that return current vat, if it is present, 
or new daemon vat if it is not present. Differently from JDK, the default is a daemon vat instead 
for ForkJoin pool, because the framework is oriented on interaction with external services 
(that could block threads in some cases) rather than for CPU-bound computations.

## Promise

`Promise` is similar in role to `CompletableFuture` that provides additional restrictions compared with 
`CompletableFuture`. It does not support `get()` operation directly to discourage it, and it does not 
permit changing result in midway.

A `Promise` could be wrapped into `CompletableFuture` and it could be created from any `CompletableStage` 
(including `CompletableFuture`), when it is needed to integrate with external services. Operations on 
`Promise` are created to encourage correct usage of it.

The promise outcome is represented by `Outcome` class that has `Failure` and `Success` subclasses.
If promise is not resolved, its outcome is null.

Linked with promise is `AResolver` interface, that could act as a listener to the promise, and also to specify 
outcome for `Promise`. Only other way to specify outcome for promise is to pass it to the constructor of promise.

There are three versions of method that adds listener to promise:
* `listenSync(AResolver)` - adds listener for `Promise` that is notified in the execution context 
  where promise is resolved. This method should be only used, if listener already has appropriate
  synchronizations or asynchronous event delivery implemented (for example, a resolver for other promise). 
* `listen(AResolver, Executor)` - adds listener for `Promise` that is notified in the context of executor.
* `listen(AResolver)` - adds listener for `Promise` that is notified in the context of default executor 
  where listener is registered.
  
There are also some utility methods on the promise that help its usage and contain some optimizations.
* `flatMap` - converts value when promise is successful with `AFunction` 
* `flatMapOutcome` - converts outcome when promise is resolved with `AFunction` 
* `map` - converts value when promise is successful with `Function`
* `mapOutcome` - converts outcome when promise is resolved with `Function` 

There are few more utility methods.

These functions are executed immediately, if result is available and with default execution context. 

The lambdas passed to these methods are executed in default execution context.

# Structured Asynchronous Programming

The core concept of the framework is asynchronous operation. *Asynchronous operation* is a sequence 
of logically grouped execution of the  

Asynchronous operators are static methods that usually return `Promise` and start with prefix `a` 
(for example `aValue`). The operations are supposed to be imported using static import to form a DSL
in the programming language.

The structured programming constructs are inspired by combining ideas from two sources:
* [E programming language](http://www.e-elang.org)
* [Occam programming language](https://en.wikipedia.org/wiki/Occam_(programming_language))

## Asynchronous Functions

Most of operators expect lambdas are arguments. These function interfaces are located at package
`org.asyncflows.core.function`. These functions return `Promise`.

* `ASupplier` - the suppler interface (analog of `Supplier`)
* `AFunction` - the single argument function interface (analog of `Function`) 
* `AFunction2` - the two argument function interface (analog of `BiFunction`)
* `AFunction3` - the three argument function interface
* `AFunction4` - the four argument function interface

## Asynchronous Context

While much of the framework functionality is able to work w/o current vat, it is best to provide a context
vat. The most simple way to do so is using AsyncContext class to create temporary local context to implement
some operation.

```$java
Integer i = doAsync(() -> aValue(42));
assertEquals(42, i);
``` 
The operation above creates `SingeThreadedVat`, run it on current thread, and then stops vat when `CompletableStage`
is done with success or failure. If it is done with success, operation exits with value, otherwise it throws
`AsycExecutionException`.  

## Trivial

Trivial operations are just different way to construct promise. Generally, the code should not need to create
promise directly, except for few DSL cases. Use promise construction operation instead. All these trivial
operations are implemented in `Promise` class as they are mostly factory methods for it.

```$java
aValue(42) // resolved promise that holds specified value
aFailure(new NullPointerException) // failed promise
aNull() // promise holding null
aVoid() // null promise with Void type.
aTrue() // promise holding true
aFalse() // promise holding false
aResolver(r -> r.accept(null, new NullPointerException())) // return promise, and to some things with resolver in body
aNow(()->aValue(a * b)) // evaluate body and return promise (if body failed, return failed promise)
aLater(()->aValue(a * b)) // evalute on later turn in default vat 
aLater(vat, ()->aValue(a * b)) // evalute on later turn in the specified vat
aNever() // the process that never ends
```

Note, `aNow` looks like useless operation, but it is actually used quite often. In many cases when constructing
asynchronous operations, throwing an error is not acceptable behaviour because some listeners are not added
and so on.

## Sequential Processes

All sequential controls method now require that they should be running in the context of the vat.

### aSeq Operator

The operator `aSeq` is basic way ot organize actions sequentially.
This operator is basically builder for sequential action. The building process starts with the initial 
action provided as `ASupplier`. Then it is continued with the following methods:

* `map(AFunction<T, N>)` - map the current result using provided function. The action is executed only 
  if previous action was success. 
* `thenDo(ASupplier<N>)` - discard the result and execute action instead. This method is useful if 
  the current result is not needed (for example it is of type Void).
* `failed(AFunction<T, AThrowable>)` - if one of previous actions failed, this method is executed, otherwise 
  it is skipped. There might be more than one `failed(...)` method in the chain.
* `mapOutcome(AFunction<N, Outcome<T>)` - this method is always invoked after block, it maps outcome of operation
  (whether if is failure or success). This method is combination of `failed()` and `map()`.
* `listen(AResolver<T>)` - this is an utility callback is that used mostly for debugging purposes. This operation
  could not be last.
  It just adds listener to promise returned from previous operations. 
* `finallyDo(ASupplier<Void>)` - this method is always invoked after block, it acts like finally clause in Java 
  try statement. It should be used mostly for clean up. This statement should be always last, and it terminates
  building process.
* `finish()` - finish building and start process. 

The following test demonstrate its usage:
```$java
        final ArrayList<Integer> list = new ArrayList<>();
        final int rc = doAsync(() ->
                aSeq(() -> {
                    list.add(1);
                    return aValue(1);
                }).map(value -> {
                    list.add(value + 1);
                    throw new IllegalStateException();
                }).thenDo(() -> {
                    // never called
                    list.add(-1);
                    return aValue(-1);
                }).failed(value -> {
                    assertEquals(IllegalStateException.class, value.getClass());
                    list.add(3);
                    return aValue(42);
                }).finallyDo(() -> {
                    list.add(4);
                    return aVoid();
                }));
        assertEquals(42, rc);
        assertEquals(Arrays.asList(1, 2, 3, 4), list);
```

There are also the following suffixes possible:
* `Last` - the function is combination of function w/o suffix, and `finish()` operation.    


### Simple Loops

The simplest loop is aSeqWhile. This loop is executed while its body returns true.
```$java
        final int rc = doAsync(() -> {
            final int[] sum = new int[1];
            final int[] current = new int[1];
            return aSeqWhile(() -> {
                sum[0] += current[0];
                current[0]++;
                return aBoolean(current[0] <= 4);
            }).thenFlatGet(() -> aValue(sum[0]));
        });
        assertEquals(10, rc);
```

There is also `Maybe` type in the framework that represent the optional value. Differently from Java `Optional`,
the `Maybe` type could hold any value including null value. It also could be serialized, passed as parameter etc.

It is possible to iterate until the value is available with this aSeqUntilValue loop.

```$java
        final int rc = doAsync(() -> {
            final int[] sum = new int[1];
            final int[] current = new int[1];
            return aSeqUntilValue(() -> {
                sum[0] += current[0];
                current[0]++;
                return current[0] <= 4 ? aMaybeEmpty() : aMaybeValue(sum[0]);
            });
        });
        assertEquals(10, rc);
```

### Collections Loops

It is possible to iterate over collections using iterator.

```$java
        final int rc = doAsync(() -> {
            final int[] sum = new int[1];
            return aSeqForUnit(Arrays.asList(0, 1, 2, 3, 4), value -> {
                sum[0] += value;
                return aTrue();
            }).thenFlatGet(() -> aValue(sum[0]));
        });
        assertEquals(10, rc);
```

It is also possible to supply iteration values to collector, but in that case it is not possible
to abort the loop:

```$java
        final int rc = doAsync(() ->
                aSeqForCollect(Stream.of(1, 2, 3, 4),
                        e -> aValue(e + 1),
                        Collectors.summingInt((Integer e) -> e))
        );
        assertEquals(14, rc);
```


The more advanced collection processing could be done by the stream framework.

## Simultaneous Processes

Sequential execution is not that interesting in asynchronous context. More interesting is case
when asynchronous operations overlap. And it could happen in the context of the same event loop.
AsyncFlows provides a number of methods to organize simultaneous asynchronous activity.

### aAll Operator

The simplest form is aAll operator. The operator starts all its branches on the current vat
on the current turn and executes `map(...)` operation when all branches are finished. If some branch
thrown exception, the operator throws an error, but it will still wait for all branches to complete.

```$java
        final Tuple2<String, Integer> rc = doAsync(() ->
                aAll(
                        () -> aValue("The answer")
                ).and(
                        () -> aLater(() -> aValue(42))
                ).map((a, b) -> aValue(Tuple2.of(a, b))));
        assertEquals(Tuple2.of("The answer", 42), rc);
```

It is possible to return tuple from all arguments directly using `Last` suffix on the last branch.

```$java
        final Tuple2<String, Integer> rc = doAsync(() ->
                aAll(
                        () -> aValue("The answer")
                ).andLast(
                        () -> aLater(() -> aValue(42))
                ));
        assertEquals(Tuple2.of("The answer", 42), rc);
```

### Processing Collections

Basic operation for iterating collection, streams, and iterators is `aAllForCollect` operators.

```$java
        final int rc = doAsync(() ->
                aAllForCollect(Stream.of(1, 2, 3, 4),
                        e -> aValue(e + 1),
                        Collectors.summingInt((Integer e) -> e))
        );
        assertEquals(14, rc);
```
It process all branches in interleaving on the current event loop. Then summarize them 
using supplied collector.

The more advanced collection processing could be done by the stream framework.

### Parallel Processes

If `aAll` is replaced with `aPar` in the previous section, then we will get parallel operations 
provided by the framework. By default, the each branch is executed on the own new daemon vat.
But is possible to customize execution by providing an implementation of ARunner interface.

```$java
        final Tuple2<String, Integer> rc = doAsync(() ->
                aPar(
                        () -> aValue("The answer")
                ).and(
                        () -> aLater(() -> aValue(42))
                ).map((a, b) -> aValue(Tuple2.of(a, b))));
        assertEquals(Tuple2.of("The answer", 42), rc);
```

This is applicable to all other `aAll` operators. 

## Alternative Processing

The alternative processing is done using `aAny` operator. This operator starts all branches on the current
turn and waits for for the first branch to complete with error or success. The `aAny` operator is intended 
for error handling and querying alternative sources of information.

```$java
        int value = doAsync(() ->
                aAny(
                        () -> aLater(() -> aValue(1))
                ).orLast(
                        () -> aValue(2)
                )
        );
        assertEquals(2, value);
        try {
            doAsync(() ->
                    aAny(
                            () -> aLater(() -> aValue(1))
                    ).orLast(
                            () -> aFailure(new RuntimeException())
                    )
            );
            fail("Unreachable");
        } catch (AsyncExecutionException ex) {
            assertEquals(RuntimeException.class, ex.getCause().getClass());
        }
```
 
 
There is also execution mode that the `aAny` operator tries to wait for successful result if possible.

```$java
        int value = doAsync(() ->
                aAny(true,
                        () -> aLater(() -> aValue(1))
                ).orLast(
                        () -> aFailure(new RuntimeException())
                )
        );
        assertEquals(1, value);
```

The other feature of aAny operator is handling of the branches that did not reach output of `aAny` operator.
This is important when the `aAny` operator opens resources that are required to be closed. Or when exceptions
from failed branches need to be logged.

The sample below demonstrates usage of `suppressed(...)` and `suppressedFailure(...)` that could be used to
receive the abandoned results.  

```$java
        Tuple3<Integer, Throwable, Integer> t = doAsync(
                () -> {
                    Promise<Throwable> failure = new Promise<>();
                    Promise<Integer> suppressed = new Promise<>();
                    return aAll(
                            () -> aAny(true,
                                    () -> aLater(() -> aValue(1))
                            ).or(
                                    () -> aValue(2)
                            ).or(
                                    () -> aFailure(new RuntimeException())
                            ).suppressed(v -> {
                                notifySuccess(suppressed.resolver(), v);
                            }).suppressedFailureLast(ex -> {
                                notifySuccess(failure.resolver(), ex);
                            })
                    ).and(
                            () -> failure
                    ).andLast(
                            () -> suppressed
                    );
                }
        );
        assertEquals(2, t.getValue1().intValue());
        assertEquals(RuntimeException.class, t.getValue2().getClass());
        assertEquals(1, t.getValue3().intValue());

```

### Fail-fast

The `FailFast` utility class is an application of the `aAny` operator.

In some cases it is needed to fail the entire process if some operation has failed.
For example, if one asynchronous operation has already failed, the related operations
need also fail.

For that purpose, framework contains FailFast utility class. The class monitor results
of operations.

Sometimes, an operation returns the resource that require cleanup (for example open connection).
In that case ignoring resource is not a valid option. For that purpose there is cleanup operation.

Let's consider a case when we have some consumer and some provider of values. For that purpose,
we will use queue components, that will be explained later in that guide. We will assume that provider
fail, so consumer might fail to receive expected value that would terminate processing. In that case,
we would like to consumer to fail as well. For example:

```$java
        ArrayList<Integer> list = new ArrayList<>();
        doAsync(() -> {
            SimpleQueue<Integer> queue = new SimpleQueue<>();
            FailFast failFast = new FailFast();
            return aAll(
                    // () -> aSeqWhile(() -> queue.take().map(t -> {
                    () -> aSeqWhile(() -> failFast.run(queue::take).map(t -> {
                        if (t == null) {
                            return false;
                        } else {
                            list.add(t);
                            return true;
                        }
                    }))
            ).andLast(
                    () -> aSeq(
                            () -> queue.put(1)
                    ).thenDo(
                            () -> queue.put(2)
                    ).thenDo(
                            // pause
                            () -> aSeqForUnit(rangeIterator(1, 10), t -> aLater(() -> aTrue()))
                    ).thenDoLast(
                            () -> failFast.run(() -> aFailure(new RuntimeException()))
                    )
            ).mapOutcome(o -> {
                assertTrue(o.isFailure());
                assertEquals(RuntimeException.class, o.failure().getClass());
                return true;
            });
        });
        assertEquals(Arrays.asList(1, 2), list);
```
If we do queue reading like in commented out line, the test will hang up, because the consumer will never
receive the value, because supplier failed. But in uncommented line, we wrap call to `queue.take()` into
fail-fast runner. This allows us to fail all executions of fail-fast that are active or will be active.
Inside the call of `failFast.run(...)` there is any operator against common promise, if any of the 
`failFast.run(...)` fails, that promise fails as well. Otherwise it stays in unresolved state.

# Object-Oriented Programming

As we have seen in previous section, the framework support rich set of asynchronous operators that
support functional and structured asynchronous programming. And the framework also supports creation 
of asynchronous components, so normal object-oriented programming could be used as well.

## Classes and Interfaces

The asynchronous interface is normal Java interface that has methods that return Promise or void.
The other types of methods could present on the interface, but they will not be supported by runtime
and they will throw an exception. Lets consider a simple Queue interface:

```$java
public interface ATestQueue<T> {
    Promise<T> take();
    void put(T element);
}
```

The method `put(...)` is one way, the method is one-way is just for demonstration here. AQueue component
in the library returns Promise<Void> because there might be errors on put operations.  
And the method `take()` returns the `Promise` as it might need to wait until some value 
is available. By convention, the interface names start with 'A' to indicate that is asynchronous 
interface.    

```$java
public class TestQueue<T> implements ATestQueue<T>, NeedsExport<ATestQueue<T>> {
    private final Deque<T> elements = new LinkedList<>();
    private final Deque<AResolver<T>> resolvers = new LinkedList<>();

    private void invariantCheck() {
        // checks that queue invariant holds
        if(!elements.isEmpty() && !resolvers.isEmpty()) {
            throw new RuntimeException("BUG: one of the collections should be empty");
        }
    }

    @Override
    public Promise<T> take() {
        invariantCheck();
        if (elements.isEmpty()) {
            return aResolver(r -> {
                resolvers.addLast(r);
            });
        } else {
            return aValue(elements.removeFirst());
        }
    }

    @Override
    public void put(final T element) {
        invariantCheck();
        if (resolvers.isEmpty()) {
            elements.addLast(element);
        } else {
            notifySuccess(resolvers.removeFirst(), element);
        }
    }

    @Override
    public ATestQueue<T> export(final Vat vat) {
        return ObjectExporter.export(vat, this);
    }
}
```

The basic idea of the implementation is that we have two queues, queue of values and queue of waiters for value.
Only one of the queues could contain values at the same time.

The method `take()` just returns the value if value is available, but if value is not available, it returns not resolved
promise and saves resolver to queue of resolvers.

The method `put(...)` checks if there is some resolver and if there is, the waiter is notified and value 
is supplied to requester. Otherwise, the value is saved. If invariant of put method fails, the error will be logged
by AsyncFlows framework, but caller will not receive it. This is why one-way methods should be generally avoided.

The class also implements interface `NeedsExport`. This interface indicates that class is not safe to use outside 
of the vat and it should be generally exported. The basic exporter is ObjectExporter, but some classes implement
optimized exporters now. The current implementation uses reflection, but runtime code generation is planned for
future. The method export, exports class to runtime.

The exporter could be written manually, and would look like this:

```$java
    public static <T> ATestQueue<T> exportTestQueue(final ATestQueue<T> service, final Vat vat) {
        return new ATestQueue<T>() {
            @Override
            public Promise<T> take() {
                return aLater(vat, () -> service.take());
            }

            @Override
            public void put(T element) {
                aSend(vat, () -> put(element));
            }
        };
    }
``` 

Let's test this method:

```$java
        final int rc = doAsync(() -> {
            final ATestQueue<Integer> queue = new TestQueue<Integer>().export();
            return aAll(() -> aSeqForUnit(rangeIterator(0, 10), i -> {
                queue.put(i + 1);
                return aTrue();
            })).and(() -> aSeqForCollect(rangeIterator(0, 10),
                    i -> queue.take(),
                    Collectors.summingInt((Integer i) -> i))
            ).selectValue2();
        });
        assertEquals((11 * 10) / 2, rc);
```
## Garbage Collection Consideration

The framework objects are generally garbage collected by Java. There is no need to perform explicit cleanup
for them, if they do not hold any sensitive resources like IO streams.

The object is prevented from garbage collection in the following cases:
* There is a direct reference to object or its proxy
* There is an event on the queue that references the object
* There is listener registered to some uncompleted promise, that is held by external listener.
  This usually means that there is some asynchronous operation is in progress.
  
Generally, the rules for garbage collection are the same as for normal Java code. But we also need
to consider promise chains as call stack. So references held by promises should be considered as
stack references to objects.

The vat object is shared between many AsyncFlows objects and asynchronous operators. The Vat might 
need to be stopped. But this usually apply to Vats that occupy thread like `SelectorVat` or `SingleThreadVat`.
Even for these vats starting/stopping is handled by the utility methods `doAsync(...)` 
and `SelectorVatUtil.run(...)`.

## Concurrency Considerations

It is assumed that asynchronous operations do not invoke blocking functionality. So many simultaneous asynchronous
operations will safely take their turns on the single queue. However, it is not always so as some operations
require calls of non-asynchronous API or to perform CPU-intensive operations.

CPU-bound operations should be generally delegated to the ForkJoin pool (`aForkJoinGet(...)`). 
IO-bound synchronous operations should be delegated to daemon thread pool (`aDaemonGet(...)`). 
If you are in doubt, just send it to daemon pool. There are helps that start operations on 
corresponding pools using vats. These operations do not establish asynchronous context
on corresponding pools, so they are quite lightweight and suitable to invocation of some 
synchronous method.

If asynchronous context need to be established, it is better to use `aLater(Vats.daemonVat(), ...)`
or `aLater(Vats.forkJoinVat(), ...)`. These operations will create a new vats that runs over corresponding
pools. 

## Request Queue

In the queue sample, the asynchronous operations are written in the way, that no new problems will happen if 
method will be called before some previous method finishes. In Java synchronous code this is usually handled
by synchronized framework. In this framework similar functionality is provided by `RequestQueue`. Biggest difference
from Java synchronization is that nested invocations of request queue are blocked.

The basic method of `RequestQueue` is `run(ASupplier<T>)`, this method has some utility variants like 
`runSeqWhile(...)`. This method executes method if request queue is empty and no method is executing 
currently, and suspends execution putting it to the queue if there is some execution in progress. 
So it is some kind of private event queue, but more flexible. There are also suspend/resume utility methods 
that are analogs of Java wait/notify.   

As example, lets consider `Semaphore` implementation similar to Java `Semaphore` class.

```$java
public interface ASemaphore {
    void release(int permits);
    void release();
    Promise<Void> acquire();
    Promise<Void> acquire(int permits);
}
```

The class in the library is implemented like the following:

```$java
public final class Semaphore implements ASemaphore, NeedsExport<ASemaphore> {
    private final RequestQueue requests = new RequestQueue();
    private int permits;

    public Semaphore(final int permits) {
        this.permits = permits;
    }

    @Override
    public void release(final int releasedPermits) {
        if (releasedPermits <= 0) {
            return;
        }
        permits += releasedPermits;
        requests.resume();
    }

    @Override
    public void release() {
        release(1);
    }

    @Override
    public Promise<Void> acquire() {
        return acquire(1);
    }

    @Override
    public Promise<Void> acquire(final int requestedPermits) {
        if (requestedPermits <= 0) {
            return aFailure(new IllegalArgumentException("The requestedPermits must be positive: " + requestedPermits));
        }
        return requests.runSeqWhile(() -> {
            if (requestedPermits <= permits) {
                permits -= requestedPermits;
                return aFalse();
            } else {
                return requests.suspendThenTrue();
            }
        });
    }

    @Override
    public ASemaphore export(final Vat vat) {
        return UtilExporter.export(vat, this);
    }
}
```

The method `acquire(...)` needs to be ordered to implement FIFO ordering. Some parts of the method 
do not need to be protected, and we can check input as we please. The rest of method is protected loop.
In the loop we check if there are permits available, and if they are, we just stop loop and this cause promise
returned by run method to resolve as well. But if they are not available, we suspend execution, and we repeat
operation when suspend ends.

The operation `release(...)` does not need to be ordered. So it is not protected by request queue. The release method
invokes `requests.resume()` to notify `acquire(...)` requests that new permits were added. The promise returned from
suspend resolves on it, and the acquire loop continues. New amount of permits might be sufficient or not. 
It is decided in the context of the acquire operation. If there is no acquire operation pending, 
the resume operation is doing nothing.   

Let's see how it works in test:

```$java
        final ArrayList<Integer> result = new ArrayList<>();
        final Void t = doAsync(() -> {
            final ASemaphore semaphore = new Semaphore(0).export();
            //noinspection Convert2MethodRef
            return aAll(() ->
                            aSeq(
                                    () -> semaphore.acquire().listen(o -> result.add(1))
                            ).thenDo(
                                    () -> semaphore.acquire(3).listen(o -> result.add(2))
                            ).thenDoLast(
                                    () -> semaphore.acquire().listen(o -> result.add(3))
                            )
            ).andLast(() ->
                    aSeq(
                            () -> aForRange(0, 10).toVoid()
                    ).thenDo(() -> {
                        result.add(-1);
                        semaphore.release(2);
                        return aVoid();
                    }).thenDo(
                            () -> aForRange(0, 10).toVoid()
                    ).thenDo(() -> {
                        result.add(-2);
                        semaphore.release();
                        return aVoid();
                    }).thenDo(
                            () -> aForRange(0, 10).toVoid()
                    ).thenDoLast(() -> {
                        result.add(-3);
                        semaphore.release(3);
                        return aVoid();
                    })).toVoid();
        });
        assertSame(null, t);
        assertEquals(Arrays.asList(-1, 1, -2, -3,  2, 3), result);
```  

# Library

## Streams

Streams library is similar to Java stream library, but there are some key differences. The first obvious difference 
is that asynchronous streams provide asynchronous stream access operations. The second difference is API design.

### Pull Streams

Asynchronous streams provide two lean interfaces and there is no intention to provide additional operations here.

```$java
public interface AStream<T> extends ACloseable {
    Promise<Maybe<T>> next();
}

public interface ASink<T> extends ACloseable {
    Promise<Void> put(T value);
    Promise<Void> fail(Throwable error);
    Promise<Void> finished();
}
```

The stream operations like map, flatMap, filter, and others are provided by stream builders. Work with StreamBuilder
typically starts with some `AsyncStreams` class method like `aForRange` or `aForStream`. Stream building starts 
in `pull` mode. So all elements will be processed sequentially. The stream builder supports typical stream operations
like `map`, `filter`, `flatMap`, `leftFold`, and `collect`. These operations accept asynchronous operations instead of 
synchronous ones.

```$java
        final int rc = doAsync(() ->
                aForRange(0, 11)
                        .filter(i -> aBoolean(i % 2 == 0))
                        .map(i -> aValue(i / 2))
                        .collect(Collectors.summingInt(e -> e))
        );
        assertEquals(15, rc);
```
Some methods also have `Sync` variant that accept Java functional interfaces.

```$java
        final int rc = doAsync(() ->
                aForRange(0, 11)
                        .filterSync(i -> i % 2 == 0)
                        .mapSync(i -> i / 2)
                        .collect(Collectors.summingInt(e -> e))
        );
        assertEquals(15, rc);
``` 

It is also possible to specify processing window. This window is basically prefetch buffer
for sequential stream. If several stages take long time, it is reasonable to start processing
next records at advance up to specified limit. The example below specifies that exactly one element
is prefetched. The sample is also shows usage of `process(...)` method that could be used to implement
reusable parts of processing pipeline

```$java
        final Function<StreamBuilder<Integer>, StreamBuilder<Integer>> delay =
                s -> s.map(a -> aForRange(0, 10).toVoid().thenValue(a));
        List<Integer> result = new ArrayList<>();
        final int rc = doAsync(() ->
                aForRange(0, 10)
                        .filter(i -> aBoolean(i % 2 == 0))
                        .mapSync(a -> {
                            result.add(a);
                            return a;
                        })
                        .window(1)
                        .process(delay)
                        .mapSync(a -> {
                            result.add(-a);
                            return a;
                        })
                        .map(i -> aValue(i / 2))
                        .collect(Collectors.summingInt(e -> e))
        );
        assertEquals(10, rc);
        assertEquals(Arrays.asList(0, 2, -0, 4, -2, 6, -4, 8, -6, -8), result);
```

### 'All' Streams

The all stream process values in the same way, but the difference is that all steps between `.all()` call
and final processing of values (or switch to `pull()`) are always processed, even in case of failures. This allows
to ensure processing of group of objects even in case of failures. For example, to close a collection of streams,
even if close operation on some of them fail.

Like for `aAll*` operators, the processing done is parallel for all elements. However, it is possible to limit 
amount of parallel processing using `.window(n)` call. In that case only several elements will be processed 
at the same time. This might be useful if the task is taxing on resources.

```$java
        final int rc = doAsync(() ->
                aForRange(0, 11)
                        .all(2)
                        .filterSync(i -> i % 2 == 0)
                        .mapSync(i -> i / 2)
                        .collect(Collectors.summingInt(e -> e))
        );
        assertEquals(15, rc);
```

Note, while each stage is parallel, the current implementation waits until previous element was passed 
to next stage before passing element to next stage. This might introduce delays to processing, 
but maintain the same order as pull stream processing. More optimized solution might be developed later. 

### Working with resources

Stream is closeable resource, and it is possible to work with stream and other closeable resources with
`aTry` statement similar to Java language `try` statement. The try statement accepts resource references,
promises for resource references, and actions that open resources. Then it closes resource after 
it has been used. Let's define a simple resource.

```$java
    public static class SampleResource implements ACloseable, NeedsExport<ACloseable> {
        private final Cell<Boolean> closed;

        public SampleResource(final Cell<Boolean> closed) {
            this.closed = closed;
        }

        @Override
        public Promise<Void> close() {
            closed.setValue(true);
            return aVoid();
        }

        @Override
        public ACloseable export(final Vat vat) {
            return () -> ResourceUtil.closeResource(vat, SampleResource.this);
        }
    }
```

This resource just support close action. Also, to support work with resources there are classes 
CloseableBase and ChainedCloseableBase that simplify creating resource wrappers. Now, we could try
different options of working with resources:

```$java
        final Cell<Boolean> r1 = new Cell<>(false);
        final Cell<Boolean> r2 = new Cell<>(false);
        final Cell<Boolean> r3 = new Cell<>(false);
        doAsync(() -> aTry(
                () -> aValue(new SampleResource(r1).export())
        ).andChain(
                value -> aValue(new SampleResource(r2).export())
        ).andChainSecond(
                value -> aValue(new SampleResource(r3).export())
        ).run((value1, value2, value3) -> aVoid()));
        assertTrue(r1.getValue());
        assertTrue(r2.getValue());
        assertTrue(r3.getValue());
```

Up to three resources could opened with one `aTry` operator. However, it is also possible
to nest `aTry` operators, so previously opened resources are accessible in lexical scope.

## IO Library

### Core IO

The IO library is also built upon lean interfaces and different operations built upon it.
The following are core interfaces of the library:

```$java
public interface AInput<B extends Buffer> extends ACloseable {
    Promise<Integer> read(B buffer);
}
public interface AOutput<B extends Buffer> extends ACloseable {
    Promise<Void> write(B buffer);
    Promise<Void> flush();
}
public interface AChannel<B extends Buffer> extends ACloseable {
    Promise<AInput<B>> getInput();
    Promise<AOutput<B>> getOutput();
}
```

As you could see, these interfaces are suitable for both character IO and 
byte IO. Some operations that work with these interfaces are 
[generic](asyncflows-io/src/main/java/org/asyncflows/io/IOUtil.java).

The following functionality is supported out of the box:
* Character encoding([DecoderInput](asyncflows-io/src/main/java/org/asyncflows/io/text/DecoderInput.java)) 
   / decoding([EncoderOutput](asyncflows-io/src/main/java/org/asyncflows/io/text/EncoderOutput.java)) 
* Digesting ([DigestingInput](asyncflows-io/src/main/java/org/asyncflows/io/util/DigestingInput.java) and
  [DigestingOutput](asyncflows-io/src/main/java/org/asyncflows/io/util/DigestingOutput.java))
* GZip ([GZipInput](asyncflows-io/src/main/java/org/asyncflows/io/util/DigestingInput.java) and
  [GZipOutput](asyncflows-io/src/main/java/org/asyncflows/io/util/DigestingOutput.java))
* Deflate ([DeflateOutput](asyncflows-io/src/main/java/org/asyncflows/io/util/DeflateOutput.java))) 
  and Inflate ([InflateInput](asyncflows-io/src/main/java/org/asyncflows/io/util/InflateInput.java)))
* Utility streams
* Synchronous stream [adapters](asyncflows-io/src/main/java/org/asyncflows/io/adapters).

### Network Library

There are two implementations of socket library based on traditional blocking sockets and selector library.
The later an implementation based on asynchronous sockets is planned.

Implementation based on traditional blocking sockets API sometimes hangs on Windows, so it is not recommended to use
if runtime also supports selector sockets. This implementation is left only backward compatibility with non-complete 
Java runtimes. 

The sockets are just byte channels with few additional operators, and they support the same operations.
But there are few additional operations.

```$java
public interface ASocket extends AChannel<ByteBuffer> {
    Promise<Void> setOptions(SocketOptions options);
    Promise<Void> connect(SocketAddress address);
    Promise<SocketAddress> getRemoteAddress();
    Promise<SocketAddress> getLocalAddress();
}
public interface AServerSocket extends ACloseable {
    Promise<SocketAddress> bind(SocketAddress address, int backlog);
    Promise<SocketAddress> bind(SocketAddress address);
    Promise<Void> setDefaultOptions(SocketOptions options);
    Promise<SocketAddress> getLocalSocketAddress();
    Promise<ASocket> accept();
}
public interface ASocketFactory {
    Promise<ASocket> makeSocket();
    Promise<AServerSocket> makeServerSocket();
    Promise<ADatagramSocket> makeDatagramSocket();
}
public interface ADatagramSocket extends ACloseable {
    Promise<Void> setOptions(SocketOptions options);
    Promise<Void> connect(SocketAddress address);
    Promise<Void> disconnect();
    Promise<SocketAddress> getRemoteAddress();
    Promise<SocketAddress> getLocalAddress();
    Promise<SocketAddress> bind(SocketAddress address);
    Promise<Void> send(ByteBuffer buffer);
    Promise<Void> send(SocketAddress address, ByteBuffer buffer);
    Promise<SocketAddress> receive(ByteBuffer buffer);
}
``` 

These interfaces could be used in the way similar to traditional synchronous code.
See [echo server](asyncflows-io/src/test/java/org/asyncflows/io/net/samples/EchoServerSample.java) 
and [echo client](asyncflows-io/src/test/java/org/asyncflows/io/net/samples/EchoClientSample.java) 
as examples.


### TLS support

TLS implementation relies on Java SSLEngine for asynchronous processing, so it follows all restrictions
enforced by it. Note, SSL protocols are not not supported by Java's SSLEngine anymore, so the framework
stick with TLS name.

The TLS implementation is just a ASocketFactory that wraps other socket factory. Interfaces are the same 
as for sockets with two additional operations on the socket:

```$java
public interface ATlsSocket extends ASocket {
    Promise<Void> handshake();
    Promise<SSLSession> getSession();
}
``` 
First one allows initiating handshake, the second one allows accessing session and examining certificates.

There are no TLS related parameters on TlsSocket factory, instead there are a factory methods for SSLEngine
which allow configuring needed parameters for SSLEngine before using it in the processing:

```$java
public class TlsSocketFactory implements ASocketFactory, NeedsExport<ASocketFactory> {
    public void setServerEngineFactory(final AFunction<SocketAddress, SSLEngine> serverEngineFactory) {
       ...
    }
    public void setClientEngineFactory(final AFunction<SocketAddress, SSLEngine> clientEngineFactory) {
        ...
    }
}
```
These factories need to configure TLS parameters basing on SocketAddress. It is expected, that different
TlsSocketFactory instances will be used for different security contexts. 

### HTTP 1.1 support

The framework provides experimental support for HTTP 1.1 protocol on client and server side.
The code is currently more like low-level protocol implementation rather than ready to use
application server. The neither side is finished, but it could be experimented with. 
HTTPS is not implemented at the moment.

See [unit test](asyncflows-protocol-http/src/test/java/org/asyncflows/protocol/http/core)
for sample code.


### Back pressure

Many asynchronous libraries have a back pressure problem. When one source of data provides more 
data than consumer might consume. Some frameworks did not had a solution for the problem 
(like Netty before 4.0), some introduce unnatural solutions like disabling/enabling reading
(like Vert.x and modern Netty), some hide it inside framework (like Akka), or provide a separate
event listeners for channels (like Apache HttpCore Async 5.x). 

However, there is no such problem with synchronous io in Java, as streams block if nothing 
could be written to it:

```$java
long lenght = 0;
byte[] b = new byte[4096]
while(true)  {
   int c = in.read(b)
   if(c < 0) {
      break;
   }
   lenght += c;
   out.write(b, 0, c);
}
return lengh;
```
That is practically all. Back pressure propagates naturally via blocking. No more data will be read,
if write is not complete. If there is error, it will be propagated to caller.

The framework provides practically the same approach. There is no explicit backpressure control. 
The output stream is accepting request, and return to caller when it is finished processing it, 
including sending data to downstream.

```$java
    public final Promise<Long> copy(final AInput<ByteBuffer> input, final AOutput<ByteBuffer> output, int bufferSize) {
        ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
        final long[] result = new long[1];
        return aSeqWhile(
                () -> input.read(buffer).flatMap(value -> {
                    if (isEof(value)) {
                        return aFalse();
                    } else {
                        result[0] += +value;
                        buffer.flip();
                        return output.write(buffer).thenFlatGet(() -> {
                            buffer.compact();
                            return aTrue();
                        });
                    }
                })
        ).thenGet(() -> result[0]);
    }
```

There are more code as asynchronous operations need to be handled and working with buffers is more complex 
than with arrays, but still it is very similar to what is written for synchronous streams.

Such way of handling back pressure does not necessary limit parallelism. It is possible to use features of the
framework to ensure that reads and writes are done in parallel when it makes sense.

```$java
    public static Promise<Long> copy(final AInput<ByteBuffer> input, final AOutput<ByteBuffer> output, int buffers, int bufferSize) {
        final SimpleQueue<ByteBuffer> readQueue = new SimpleQueue<>();
        final SimpleQueue<ByteBuffer> writeQueue = new SimpleQueue<>();
        final FailFast failFast = failFast();
        for (int i = 0; i < buffers; i++) {
            readQueue.put(ByteBuffer.allocate(bufferSize));
        }
        final long[] result = new long[1];
        return aAll(
                () -> aSeqWhile(
                        () -> failFast.run(readQueue::take).flatMap(
                                b -> failFast.run(() -> input.read(b)).flatMap(c -> {
                            if (isEof(c)) {
                                writeQueue.put(null);
                                return aFalse();
                            } else {
                                result[0] += c;
                                writeQueue.put(b);
                                return aTrue();
                            }
                        }))
                )
        ).and(
                () -> aSeqWhile(
                        () -> failFast.run(writeQueue::take).flatMap(b -> {
                            if(b == null) {
                                return aFalse();
                            } else {
                                b.flip();
                                return failFast.run(() -> output.write(b)).thenGet(() -> {
                                    b.compact();
                                    readQueue.put(b);
                                    return true;
                                });
                            }
                        })
                )
        ).map((a, b) -> aValue(result[0]));
    }
```
In the provided sample, the read operation uses buffers to read when available, and writes when buffer with 
data is available. So if writes are slower or reads are slower, the algorithm will adapt to the speed. This
algorithm makes sense with no more than four buffers, as one buffer is for reading, one for writing, and two
are in flight over the queue.

### Control flow inversion

Most of asynchronous libraries require inversion of control flow. Most of asynchronous frameworks use
concepts like decoders and encoders. These are two poor things that have to implement explicit tracking of the 
current state of reading or writing. If there is a recursive state like xml or json, they have to 
keep explicit stack of state.

# Inter-Process Communications

The AsyncFlows framework is intended to implement control flow inside the application. There is no special means 
to organize intra-process communications. However, the libraries could be used to organize such communications. 
For example, JAX-RS 2.0 supports asynchronous invocations on client and server.   

# Java EE support

TBD

## Spring Framework

TBD

## Servlets

TBD

## JAX-RS

TBD

## Java EE Security Manager

The framework uses own thread pool and it could be incompatible with Java EE when security manager is enabled.
Turn off security manager or add appropriate permissions for your application. Also, the contextual security
checks are not so valid in asynchronous context and they could be break important assumptions about security
if Java EE components are called.

The contextual security information like active user should be passed as parameters, and 
it needs to be reestablished before invocation of Java EE functionality that requires it (for example 
Hibernate audit support). 

# Comparison with other frameworks

## Actors (Akka)

Comparing with Scala actors, there are the following key points of difference.

1. In the AsyncFlows framework, component and event queue are separated and one queue could support many small components.
Practically, there is at least one one asynchronous micro-component for each asynchronous operation. In Scala, there
are only one asynchronous component for each event queue. This leads to problems with resource management as state of
component need to be tracked.

2. Event dispatch is done explicitly and each queue supports only closed set of events. There is no interfaces 
for components and even returning result is different each time. (TypedActors try to solve problem of explicit dispatch, 
but introduce own set of the problems due to blocking calls, and also still support only closed set of events).
AsyncFlows support open set of events, as they translate to `Runnable` anyway. As many components could leave 

3. Actors are heavy-weight as they are integrated with event queue. They also need to be deleted explicitly to free
resources. By comparison, AsyncFlows do not manage components explicitly, as they could garbage collected normally.
Some Vats needs to be managed explicitly, but these vats are usually used as application starting point in 
the main thread. ExecutorVat does not need to be explicitly stopped (the underlying executor needs to be stopped, 
but daemon executor creates and frees threads as needed and does not need to be stopped).

4. As Akka Actors work with event queue directly, it is possible handle events not in the order they were sent to actor.
AsyncFlows insists on handling events in the order they are received by a vat. Reordering of event
handling still could be done by utility classes like RequestQueue.

Generally, AsyncFlows support more flexible management of asynchronous components and their relationship 
to event queues. Also AsyncFlows support running the entire network application in the single thread,
while Akka requires multiple threads by design. 

## CompletableFuture

Java's CompletableFuture is similar to AsyncFlows Promise. CompletableFuture has a lot of utility methods that 
implement much of functionality similar to provided by the AsyncFlows framework. However, AsyncObjects Framework
shifts this functionality from Promise to operators that are built upon Promise (operation builders, static methods).
The difference is small, but it greatly affects usability as AsyncFlows does not need a lot of methods since
many method could be replaced by combination of existing method.

There were actually experimental version of the framework that used CompletableFuture as foundation 
instead of promise. However, this version proved to be less usable, as it is more complex to listen for events,
for example it is not possible to just to listen to CompletableFuture w/o creating another completable future.
Also the defaults for execution context are different. The framework defaults to the current Vat. 
The CompletableFuture defaults to ForkJoin pool. This pool is generally not acceptable for IO operations,
and IO could block it for indefinite time. Small errors could lead to application blocking. Practically all
invocations on CompletableFuture required explicit specification of target vat.

AsyncFlows also has a lot of utility methods, that do not make sense as CompletableFuture API. 
For example, loops, request queues, fail-fast.

Also, CompletableFuture does not have component model. It is just a single class w/o larger
contexts. When and how asynchronous method is executed is left up to component designer.   
 
## Netty

The netty is organized as multi-stage event processing. It works very well when uniform processing is needed.
The problem is that most of processing that is needed is non-uniform. There are generally recursive logical 
asynchronous processes built upon event streams. Netty requires implementing such processes using 
explicit stacks and other means.

In contrast, AsyncFlows allows to freely use recursion when needed, just like in normal synchronous code.
There is no need for inversion of control.

Up to recent versions of Netty, the netty did not support back pressure regulation, and because of 
event notification approach, there were no natural way to specify it. The current way is still 
cumbersome.

On other hand, netty contains implementation of many network protocols. And it makes sense to reuse
these implementations from AsyncFlows. There is plan to create a library that access Netty channels
from AsyncFlows framework.

# Other Programming Languages

The framework relies on Java 8 functional interfaces to create DSL. So if other language supports them 
in reasonable way, it is possible to use this DSL language in similar way.

## Groovy

Groovy since version 2.4 supports java functional interfaces using closure syntax. However, sometimes more
type annotations are needed, to specify parameter types if type checking is wanted. The syntax actually looks
more nice for groovy.  

```$groovy
        def t = doAsync {
            def failure = new Promise<Throwable>();
            def suppressed = new Promise<Integer>();
            aAll {
                aAny(true) {
                    aLater { aValue(1) }
                } or {
                    aValue(2)
                } or {
                    aFailure(new RuntimeException())
                } suppressed {
                    notifySuccess(suppressed.resolver(), it)
                } suppressedFailureLast {
                    notifySuccess(failure.resolver(), it);
                }
            } and {
                failure
            } andLast {
                suppressed
            }
        }
        assertEquals(2, t.getValue1().intValue());
        assertEquals(RuntimeException.class, t.getValue2().getClass());
        assertEquals(1, t.getValue3().intValue());
```

There is much less visual noise in groovy version than in Java version of the same test.
The Groovy is a good choice of using with the framework if there is no special concerns about
performance.

Note, Groovy currently implements lambdas using inner classes, so more classes are generated comparing 
to Java 8 code. This might lead to higher application start time.  

## Kotlin

The Kotlin language also has compact syntax that support DSL creation. It is also possible
to write a compact code with much less visual noise in Kotlin as well.

```$kotlin
        val t = doAsync {
            val failure = Promise<Throwable>()
            val suppressed = Promise<Int>()
            aAll {
                aAny(true) {
                    aLater { aValue(1) }
                }.or {
                    aFailure(RuntimeException())
                }.or {
                    aValue(2)
                }.suppressed { v ->
                    notifySuccess(suppressed.resolver(), v)
                }.suppressedFailureLast { ex ->
                    notifySuccess<Throwable>(failure.resolver(), ex)
                }
            }.and {
                failure
            }.andLast {
                suppressed
            }
        }
        assertEquals(2, t.value1)
        assertEquals(RuntimeException::class.java, t.value2.javaClass)
        assertEquals(1, t.value3)
```

So Kotlin is also good language to write structured asynchronous code if you project allows for it.
It is very convenient for the application and test code, but for reusable code it needs to be considered 
more carefully.

Note, Kotlin currently implement lambdas using inner classes, so more classes are generated comparing 
to Java 8 code. This might lead to higher application start time.  

## Scala

The Scala is not directly supported as it wraps Java types and this causes multiple problems 
in different places. So for the Scala adapters needed and support for scala collections needs 
to be implemented. Some code could be executed directly, but it is less usable than in other
languages.

Generally, the framework ideas are compatible with Scala, and few first research versions of 
the framework were implemented in Scala. This Java version is based on ideas from Scala version. 
And Java 8 finally allows more compact syntax to be used.

The future versions of the framework might provide Scala support after the framework 
stabilization. However, comparing to Kotlin and Groovy, there is not so big productivity
increase and there even some additional complications cased by features of Scala language.
So this feature has low priority. There is previous iteration of scala adapter at 
[this link](https://github.com/const/asyncflows/tree/63586493fb9d5a63c0c335df63fa396d894b0a5b/asyncobjects-scala).

In the old sample code, control flow looked like the following:

```$scala
    val list = new ListBuffer[Integer]
    val rc: Int = doAsync {
      aSeq {
        list += 1
        1
      } map { value =>
        list += value + 1
        throw new IllegalStateException
      } thenDo {
        list += -1
        aValue(-1)
      } failed {
        case value: IllegalStateException =>
          list += 3
          42
      } finallyDo {
        list += 4
      }
    }

    assertEquals(42, rc)
    assertEquals(List(1, 2, 3, 4), list)
```

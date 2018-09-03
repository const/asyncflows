AsyncFlows Framework 0.1.0
--------------------------

The framework support easy and modular construction of asynchronous processes from simpler constructs.
The framework is mostly targeted to IO-bound processes and it is not intended for IO bound processes.

# Framework Foundations

The concept described in this section are foundations of the framework.
While they are foundation, the user of the framework rarely interacts 
with them directly, so do not assume that code samples here are anything
like what you would see in application 

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

### Default Execution Context 

When an asynchronous context is needed, but it is not clear whether the current thread has one,
It is possible to use `Vat.defaultExecutor()` method, that return current vat, if it is present, 
or new daemon vat if it is not present. Differently from JDK, the default is a daemon vat instead 
for ForkJoin pool, because the framework is oriented on interaction with external services 
(that could block threads in some cases) rather than for CPU computations.

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

# Asynchronous Operators

Asynchronous operators are static methods that usually return `Promise` and start with prefix `a` 
(for example `aValue`). The operations are supposed to be imported using static import to form a DSL
in the programming language.

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
`ExecutionException`.  

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
aLater(()->aValue(a * b), vat) // evalute on later turn in the specified vat
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

# Asynchronous Components

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

# Other Programming Languages

The framework relies on Java 8 functional interfaces to create DSL. So if other language supports them 
in reasonable way, it is possible to use this DSL language in similar way.

## Groovy

## Kotlin

## Scala

The Scala is not directly supported as it wraps Java types. So for the Scala adapters needed and support for scala
collections needs to be implemented. Some code could be executed directly, but it is less usable than in other
languages. The future versions of the framework could provide Scala support after the framework stabilization.
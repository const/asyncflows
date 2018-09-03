package org.asyncflows.core.vats;

import org.junit.jupiter.api.Test;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

public class ExecutorVatTest {

    @Test
    public void daemonVatTest() throws InterruptedException {
        Semaphore semaphore = new Semaphore(0);
        AtomicReference<Thread> thread = new AtomicReference<>();
        ExecutorVat vat = Vats.daemonVat(); // create vat
        vat.execute(() -> { // schedule a vat
            thread.set(Thread.currentThread()); // executes in other thread
            semaphore.release(); // release semaphore
        });
        semaphore.acquire(); // wait until completes
        assertNotNull(thread.get()); // actually executed
        assertNotSame(thread.get(), Thread.currentThread()); // and in different thread
    }
}

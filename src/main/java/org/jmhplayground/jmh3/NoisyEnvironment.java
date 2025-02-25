package org.jmhplayground.jmh3;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

/**
 * This benchmark shows the effect of running a benchmark in a noisy environment.
 *
 * In order to properly run experiments it's important to reduce at minimum the effects of the OS scheduler decisions
 * and governor. Such machine should have fixed or known frequencies under known conditions and the experiments should
 * try to not overcommit over its existing resources, unless it is the purpose of the experiment itself.
 *
 * Run with
 * -prof perf
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 10, time = 400, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 200, timeUnit = TimeUnit.MILLISECONDS)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(2)
public class NoisyEnvironment {

    @Param({"10"})
    private int work;

    @Param({"0", "4", "8"})
    private int noisyNeighbors;

    private ExecutorService neighborService;

    private volatile boolean shutdown;

    @Setup
    public void setup() throws InterruptedException {
        shutdown = false;
        if (noisyNeighbors > 0) {
            neighborService = Executors.newFixedThreadPool(noisyNeighbors);
            CountDownLatch allStarted = new CountDownLatch(noisyNeighbors);
            for (int i = 0; i < noisyNeighbors; i++) {
                neighborService.submit(() -> {
                    allStarted.countDown();
                    while (!shutdown) {
                        Blackhole.consumeCPU(work);
                    }
                });
            }
            allStarted.await();
        }
    }

    @TearDown
    public void tearDown() {
        if (neighborService == null) {
            return;
        }
        shutdown = true;
        neighborService.shutdownNow();
        neighborService.close();
    }

    @Benchmark
    public void consumeCpu() {
        Blackhole.consumeCPU(work);
    }
}
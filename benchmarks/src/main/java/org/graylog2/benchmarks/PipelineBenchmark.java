package org.graylog2.benchmarks;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.graylog2.benchmarks.pipeline.ClassicModule;
import org.graylog2.benchmarks.pipeline.ClassicPipeline;
import org.graylog2.benchmarks.pipeline.ProcessedMessage;
import org.graylog2.benchmarks.utils.FixedTimeCalculator;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/*
    run with  java -DbatchSize=32 -DinputBufferSize=8192 -DoutputBufferSize=8192 -Xmx512m -jar target/benchmarks.jar PipelineBenchmark -wf 0 -wi 0 -t 16 -f 1 -r 60s -si false
 */

@State(Scope.Benchmark)
public class PipelineBenchmark {

    ClassicPipeline classicPipeline;
    Counter counter;
    Meter meter;
    private int batchSize = Integer.valueOf(System.getProperty("batchSize", "10"));
    private int inputBufferSize = Integer.valueOf(System.getProperty("inputBufferSize", "2048"));
    private int inputBufferHandler = Integer.valueOf(System.getProperty("inputBufferHandler", "4"));
    private int outputBufferSize = Integer.valueOf(System.getProperty("outputBufferSize", "2048"));
    private int outputBufferHandler = Integer.valueOf(System.getProperty("outputBufferHandler", "2"));
    private int filterProcessTime = Integer.valueOf(System.getProperty("inputProcessTimeMicros", "250"));
    private int outputProcessTime = Integer.valueOf(System.getProperty("outputProcessTimeMicros", "250"));
    private boolean waitForProcessedBatch = Boolean.parseBoolean(System.getProperty("waitForProcessedBatch", "false"));

    @Setup
    public void setup() {
        final Injector injector = Guice.createInjector(new ClassicModule());
        final ClassicPipeline.Factory factory = injector.getInstance(ClassicPipeline.Factory.class);
        classicPipeline = factory.create(
                inputBufferHandler,
                new FixedTimeCalculator(filterProcessTime, TimeUnit.MICROSECONDS),
                outputBufferHandler,
                new FixedTimeCalculator(outputProcessTime, TimeUnit.MICROSECONDS),
                inputBufferSize,
                outputBufferSize);
        final MetricRegistry metricRegistry = injector.getInstance(MetricRegistry.class);
        counter = metricRegistry.counter("benchmark-iterations");
        meter = metricRegistry.meter("benchmark-ops");

        ConsoleReporter.forRegistry(metricRegistry).build().start(10, TimeUnit.SECONDS);
    }


    @Benchmark
    public void classic() throws ExecutionException {
        ProcessedMessage processedMessage = null;
        for (int i = 0; i < batchSize; i++) {
            processedMessage = classicPipeline.produce();
        }
        assert processedMessage != null;
        if (waitForProcessedBatch)
            Uninterruptibles.getUninterruptibly(processedMessage);
        counter.inc();
        meter.mark(batchSize);
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(PipelineBenchmark.class.getSimpleName())
                .warmupIterations(0)
                .measurementIterations(5)
                .measurementTime(TimeValue.minutes(1))
                .build();

        new Runner(opt).run();
    }

    public static class SimpleRun {
        public static void main(String[] args) {
            final PipelineBenchmark p = new PipelineBenchmark();
            p.setup();

            final int nThreads = 32;
            final ExecutorService service = Executors.newFixedThreadPool(nThreads);
            for (int i = 0; i < nThreads; i++) {
                service.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            while (true) {
                                p.classic();
                            }
                        } catch (ExecutionException e) {
                        }
                    }
                });
            }
            Uninterruptibles.sleepUninterruptibly(1, TimeUnit.DAYS);
        }
    }
}

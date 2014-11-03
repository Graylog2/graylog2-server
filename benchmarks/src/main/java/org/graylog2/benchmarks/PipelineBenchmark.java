package org.graylog2.benchmarks;

import com.codahale.metrics.*;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.google.common.base.Throwables;
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.inject.Guice;
import com.google.inject.Injector;
import metrics_influxdb.Influxdb;
import metrics_influxdb.InfluxdbReporter;
import org.graylog2.benchmarks.pipeline.ClassicModule;
import org.graylog2.benchmarks.pipeline.ClassicPipeline;
import org.graylog2.benchmarks.pipeline.ProcessedMessage;
import org.graylog2.benchmarks.utils.FixedTimeCalculator;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/*
    run with  java -DbatchSize=32 -DinputBufferSize=8192 -DoutputBufferSize=8192 -Xmx512m -jar target/benchmarks.jar PipelineBenchmark -wf 0 -wi 0 -t 16 -f 1 -r 60s -si false
 */

@State(Scope.Benchmark)
public class PipelineBenchmark {
    private static final Logger log = LoggerFactory.getLogger(PipelineBenchmark.class);

    ClassicPipeline classicPipeline;
    Counter counter;
    Meter meter;
    private final int batchSize = Integer.valueOf(System.getProperty("batchSize", "10"));
    private final int inputBufferSize = Integer.valueOf(System.getProperty("inputBufferSize", "2048"));
    private final int inputBufferHandler = Integer.valueOf(System.getProperty("inputBufferHandler", "4"));
    private final int outputBufferSize = Integer.valueOf(System.getProperty("outputBufferSize", "2048"));
    private final int outputBufferHandler = Integer.valueOf(System.getProperty("outputBufferHandler", "2"));
    private final int filterProcessTime = Integer.valueOf(System.getProperty("inputProcessTimeMicros", "250"));
    private final int outputProcessTime = Integer.valueOf(System.getProperty("outputProcessTimeMicros", "250"));
    private final boolean waitForProcessedBatch = Boolean.parseBoolean(System.getProperty("waitForProcessedBatch", "false"));
    private ScheduledReporter reporter;

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
        metricRegistry.registerAll(new GarbageCollectorMetricSet());
        metricRegistry.registerAll(new MemoryUsageGaugeSet());

        if (System.getProperty("useInfluxdb") != null) {
            final String host = System.getProperty("influxdbHost", "127.0.0.1");
            final String port = System.getProperty("influxdbPort", "8086");
            final String db = System.getProperty("influxdbDb", "metrics");
            final String user = System.getProperty("influxdbUser", "root");
            final String pass = System.getProperty("influxdbPassword", "root");
            Influxdb influxdb = null;
            try {
                influxdb = new Influxdb(host, Integer.valueOf(port), db, user, pass, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                Throwables.propagate(e);
            }
            final InfluxdbReporter reporter = InfluxdbReporter
                    .forRegistry(metricRegistry)
                    .prefixedWith("benchmark")
                    .convertRatesTo(TimeUnit.SECONDS)
                    .convertDurationsTo(TimeUnit.MILLISECONDS)
                    .filter(MetricFilter.ALL)
                    .build(influxdb);
            reporter.start(1, TimeUnit.SECONDS);
            log.info("Starting influxdb reporter, writing to {}", influxdb.url);
        } else {
            reporter = ConsoleReporter.forRegistry(metricRegistry).build();
            reporter.start(5, TimeUnit.SECONDS);
            log.info("Starting console reporter");
        }
    }

    @TearDown
    public void stop() {
        log.info("Tearing down benchmark");
        if (reporter != null) {
            log.info("Stopping reporter");
            reporter.stop();
        }
        classicPipeline.stop();
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

/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.buffers.processors;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.InstrumentedExecutorService;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.inject.assistedinject.Assisted;
import jakarta.inject.Inject;
import org.graylog2.Configuration;
import org.graylog2.outputs.DefaultMessageOutput;
import org.graylog2.outputs.OutputRouter;
import org.graylog2.plugin.GlobalMetricNames;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.SystemMessage;
import org.graylog2.plugin.buffers.MessageEvent;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.shared.buffers.WorkHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.codahale.metrics.MetricRegistry.name;

public class OutputBufferProcessor implements WorkHandler<MessageEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(OutputBufferProcessor.class);

    private static final String INCOMING_MESSAGES_METRICNAME = name(OutputBufferProcessor.class, "incomingMessages");
    private static final String INCOMING_SYSTEM_MESSAGES_METRICNAME = name(OutputBufferProcessor.class, "incomingSystemMessages");
    private static final String PROCESS_TIME_METRICNAME = name(OutputBufferProcessor.class, "processTime");

    private final ExecutorService executor;

    private final Configuration configuration;
    private final ServerStatus serverStatus;

    private final Meter incomingMessages;
    private final Meter incomingSystemMessages;
    private final Counter outputThroughput;
    private final Timer processTime;

    private final OutputRouter outputRouter;
    private final MessageOutput defaultMessageOutput;
    private final int processorOrdinal;

    @Inject
    public OutputBufferProcessor(Configuration configuration,
                                 MetricRegistry globalMetricRegistry,
                                 ServerStatus serverStatus,
                                 OutputRouter outputRouter,
                                 @DefaultMessageOutput MessageOutput defaultMessageOutput,
                                 @Assisted int processorOrdinal) {
        this.configuration = configuration;
        this.serverStatus = serverStatus;
        this.outputRouter = outputRouter;
        this.defaultMessageOutput = defaultMessageOutput;
        this.processorOrdinal = processorOrdinal;

        final int corePoolSize = configuration.getOutputBufferProcessorThreadsCorePoolSize();
        this.executor = executorService(globalMetricRegistry, corePoolSize);

        this.incomingMessages = globalMetricRegistry.meter(INCOMING_MESSAGES_METRICNAME);
        this.incomingSystemMessages = globalMetricRegistry.meter(INCOMING_SYSTEM_MESSAGES_METRICNAME);
        this.outputThroughput = globalMetricRegistry.counter(GlobalMetricNames.OUTPUT_THROUGHPUT);
        this.processTime = globalMetricRegistry.timer(PROCESS_TIME_METRICNAME);
    }

    private ExecutorService executorService(final MetricRegistry globalRegistry, final int corePoolSize) {

        final String nameFormat = "outputbuffer-processor-" + processorOrdinal + "-executor-%d";
        final ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat(nameFormat).build();

        // Some executor service metrics are shared between buffer processors. This is unusual but was done on
        // purpose. We'll keep the shared metrics shared but put the gauges, which can't be shared, in a separate
        // namespace.
        final String sharedPrefix = name(this.getClass(), "executor-service");
        final String uniquePrefix = name(this.getClass(), String.valueOf(processorOrdinal), "executor-service");

        ExecutorService delegate = Executors.newFixedThreadPool(corePoolSize, threadFactory);

        // Get or create shared metrics and copy them into a local registry to be re-used by the executor service
        var localRegistry = new MetricRegistry();
        localRegistry.register(name(uniquePrefix, "submitted"), globalRegistry.meter(name(sharedPrefix, "submitted")));
        localRegistry.register(name(uniquePrefix, "running"), globalRegistry.counter(name(sharedPrefix, "running")));
        localRegistry.register(name(uniquePrefix, "completed"), globalRegistry.meter(name(sharedPrefix, "completed")));
        localRegistry.register(name(uniquePrefix, "idle"), globalRegistry.timer(name(sharedPrefix, "idle")));
        localRegistry.register(name(uniquePrefix, "duration"), globalRegistry.timer(name(sharedPrefix, "duration")));

        final InstrumentedExecutorService executorService = new InstrumentedExecutorService(
                delegate,
                localRegistry,
                uniquePrefix);

        // Register gauges from the local registry in the global registry
        final Map<String, Metric> gauges = localRegistry.getMetrics().entrySet().stream()
                .filter(e -> e.getValue() instanceof Gauge)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        globalRegistry.registerAll(() -> gauges);

        return executorService;
    }

    /**
     * Each message will be written to one or more outputs.
     * <p>
     * The default output is always being used for every message, but optionally the message can be routed to additional
     * outputs, currently based on the stream outputs that are configured in the system.
     * </p>
     * <p>
     * The stream outputs are time limited so one bad output does not impact throughput too much. Essentially this means
     * that the work of writing to the outputs is performed, but the writer threads will not wait forever for stream
     * outputs to finish their work. <b>This might lead to increased memory usage!</b>
     * </p>
     * <p>
     * The default output, however, is allowed to block and is not subject to time limiting. This is important because it
     * can exert back pressure on the processing pipeline this way, making sure we don't run into excessive heap usage.
     * </p>
     *
     * @param event the message to write to outputs
     * @throws Exception
     */
    @Override
    public void onEvent(MessageEvent event) throws Exception {
        final Message msg = event.getMessage();
        if (msg == null) {
            LOG.debug("Skipping null message.");
            return;
        }
        LOG.trace("Processing message <{}> from OutputBuffer.", msg.getId());

        if (msg instanceof SystemMessage) {
            incomingSystemMessages.mark();
        } else {
            incomingMessages.mark();
        }

        final Set<MessageOutput> messageOutputs = outputRouter.getStreamOutputsForMessage(msg);
        msg.recordCounter(serverStatus, "matched-outputs", messageOutputs.size());

        final Future<?> defaultOutputCompletion = processMessage(msg, defaultMessageOutput);

        final CountDownLatch streamOutputsDoneSignal = new CountDownLatch(messageOutputs.size());
        for (final MessageOutput output : messageOutputs) {
            processMessage(msg, output, streamOutputsDoneSignal);
        }

        // Wait until all writer threads for stream outputs have finished or timeout is reached.
        if (!streamOutputsDoneSignal.await(configuration.getOutputModuleTimeout(), TimeUnit.MILLISECONDS)) {
            LOG.warn("Timeout reached. Not waiting any longer for stream output writer threads to complete.");
        }

        // now block until the default output has finished. most batching outputs will already been done because their
        // fast path is really fast (usually an insert into a queue), but the slow flush path might block for a long time
        // this exerts the back pressure to the system
        if (defaultOutputCompletion != null) {
            Uninterruptibles.getUninterruptibly(defaultOutputCompletion);
        } else {
            LOG.error("The default output future was null, this is a bug!");
        }

        if (msg.hasRecordings()) {
            LOG.debug("Message event trace: {}", msg.recordingsAsString());
        }

        outputThroughput.inc();

        LOG.debug("Wrote message <{}> to all outputs. Finished handling.", msg.getId());

        event.clearMessages();
    }

    private Future<?> processMessage(final Message msg, final MessageOutput defaultMessageOutput) {
        return processMessage(msg, defaultMessageOutput, new CountDownLatch(0));
    }

    private Future<?> processMessage(final Message msg, final MessageOutput output, final CountDownLatch doneSignal) {
        if (output == null) {
            LOG.error("Output was null!");
            doneSignal.countDown();
            return Futures.immediateCancelledFuture();
        }
        if (!output.isRunning()) {
            LOG.debug("Skipping stopped output {}", output.getClass().getName());
            doneSignal.countDown();
            return Futures.immediateCancelledFuture();
        }

        Future<?> future = null;
        try {
            LOG.debug("Writing message to [{}].", output.getClass());
            if (LOG.isTraceEnabled()) {
                LOG.trace("Message id for [{}]: <{}>", output.getClass(), msg.getId());
            }
            future = executor.submit(new Runnable() {
                @Override
                public void run() {
                    try (Timer.Context ignored = processTime.time()) {
                        output.write(msg);
                    } catch (Exception e) {
                        LOG.error("Error in output [" + output.getClass() + "].", e);
                    } finally {
                        doneSignal.countDown();
                    }
                }
            });
        } catch (Exception e) {
            LOG.error("Could not write message batch to output [" + output.getClass() + "].", e);
            doneSignal.countDown();
        }
        return future;
    }

    public interface Factory {
        OutputBufferProcessor create(@Assisted int ordinal);
    }
}

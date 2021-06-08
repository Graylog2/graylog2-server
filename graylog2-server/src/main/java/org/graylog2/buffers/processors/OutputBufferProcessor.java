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
import com.codahale.metrics.InstrumentedExecutorService;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.common.util.concurrent.Uninterruptibles;
import com.lmax.disruptor.WorkHandler;
import org.graylog2.Configuration;
import org.graylog2.outputs.DefaultMessageOutput;
import org.graylog2.outputs.OutputRouter;
import org.graylog2.plugin.GlobalMetricNames;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.buffers.MessageEvent;
import org.graylog2.plugin.outputs.MessageOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.codahale.metrics.MetricRegistry.name;

public class OutputBufferProcessor implements WorkHandler<MessageEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(OutputBufferProcessor.class);

    private static final String INCOMING_MESSAGES_METRICNAME = name(OutputBufferProcessor.class, "incomingMessages");
    private static final String PROCESS_TIME_METRICNAME = name(OutputBufferProcessor.class, "processTime");

    private final ExecutorService executor;

    private final Configuration configuration;
    private final ServerStatus serverStatus;

    private final Meter incomingMessages;
    private final Counter outputThroughput;
    private final Timer processTime;

    private final OutputRouter outputRouter;
    private final MessageOutput defaultMessageOutput;

    @Inject
    public OutputBufferProcessor(Configuration configuration,
                                 MetricRegistry metricRegistry,
                                 ServerStatus serverStatus,
                                 OutputRouter outputRouter,
                                 @DefaultMessageOutput MessageOutput defaultMessageOutput) {
        this.configuration = configuration;
        this.serverStatus = serverStatus;
        this.outputRouter = outputRouter;
        this.defaultMessageOutput = defaultMessageOutput;

        final String nameFormat = "outputbuffer-processor-executor-%d";
        final int corePoolSize = configuration.getOutputBufferProcessorThreadsCorePoolSize();
        final int maxPoolSize = configuration.getOutputBufferProcessorThreadsMaxPoolSize();
        final int keepAliveTime = configuration.getOutputBufferProcessorKeepAliveTime();
        this.executor = executorService(metricRegistry, nameFormat, corePoolSize, maxPoolSize, keepAliveTime);

        this.incomingMessages = metricRegistry.meter(INCOMING_MESSAGES_METRICNAME);
        this.outputThroughput = metricRegistry.counter(GlobalMetricNames.OUTPUT_THROUGHPUT);
        this.processTime = metricRegistry.timer(PROCESS_TIME_METRICNAME);
    }

    private ExecutorService executorService(final MetricRegistry metricRegistry, final String nameFormat,
                                            final int corePoolSize, final int maxPoolSize, final int keepAliveTime) {
        final ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat(nameFormat).build();
        return new InstrumentedExecutorService(
                new ThreadPoolExecutor(corePoolSize, maxPoolSize, keepAliveTime, TimeUnit.MILLISECONDS,
                        new LinkedBlockingQueue<Runnable>(), threadFactory),
                metricRegistry,
                name(this.getClass(), "executor-service"));
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
        incomingMessages.mark();

        final Message msg = event.getMessage();
        if (msg == null) {
            LOG.debug("Skipping null message.");
            return;
        }
        LOG.debug("Processing message <{}> from OutputBuffer.", msg.getId());

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
}

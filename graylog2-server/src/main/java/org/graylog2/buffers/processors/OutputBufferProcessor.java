/*
 * Copyright 2012-2014 TORCH GmbH
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.graylog2.buffers.processors;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lmax.disruptor.EventHandler;
import org.graylog2.Configuration;
import org.graylog2.buffers.OutputBufferWatermark;
import org.graylog2.outputs.CachedOutputRouter;
import org.graylog2.outputs.OutputRegistry;
import org.graylog2.outputs.OutputRouter;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.buffers.MessageEvent;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.shared.ServerStatus;
import org.graylog2.shared.stats.ThroughputStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.*;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class OutputBufferProcessor implements EventHandler<MessageEvent> {
    public interface Factory {
        public OutputBufferProcessor create(@Assisted("ordinal") final long ordinal,
                                            @Assisted("numberOfConsumers") final long numberOfCOnsumers);
    }

    private static final Logger LOG = LoggerFactory.getLogger(OutputBufferProcessor.class);

    private final ExecutorService executor;

    private final Configuration configuration;
    private final OutputRegistry outputRegistry;
    private final ThroughputStats throughputStats;
    private final ServerStatus serverStatus;

    //private List<Message> buffer = Lists.newArrayList();

    private final Meter incomingMessages;
    private final Histogram batchSize;
    private final Timer processTime;

    private final OutputBufferWatermark outputBufferWatermark;
    private final OutputRouter outputRouter;
    private final long ordinal;
    private final long numberOfConsumers;

    @AssistedInject
    public OutputBufferProcessor(Configuration configuration,
                                 MetricRegistry metricRegistry,
                                 OutputRegistry outputRegistry,
                                 ThroughputStats throughputStats,
                                 ServerStatus serverStatus,
                                 OutputBufferWatermark outputBufferWatermark,
                                 CachedOutputRouter outputRouter,
                                 @Assisted("ordinal") final long ordinal,
                                 @Assisted("numberOfConsumers") final long numberOfConsumers) {
        this.configuration = configuration;
        this.outputRegistry = outputRegistry;
        this.throughputStats = throughputStats;
        this.serverStatus = serverStatus;
        this.outputBufferWatermark = outputBufferWatermark;
        this.outputRouter = outputRouter;
        this.ordinal = ordinal;
        this.numberOfConsumers = numberOfConsumers;

        executor = new ThreadPoolExecutor(
            configuration.getOutputBufferProcessorThreadsCorePoolSize(),
            configuration.getOutputBufferProcessorThreadsMaxPoolSize(),
            5, TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>(),
            new ThreadFactoryBuilder()
            .setNameFormat("outputbuffer-processor-" + ordinal + "-executor-%d")
            .build());

        incomingMessages = metricRegistry.meter(name(OutputBufferProcessor.class, "incomingMessages"));
        batchSize = metricRegistry.histogram(name(OutputBufferProcessor.class, "batchSize"));
        processTime = metricRegistry.timer(name(OutputBufferProcessor.class, "processTime"));
    }

    @Override
    public void onEvent(MessageEvent event, long sequence, boolean endOfBatch) throws Exception {
        // Because Trisha said so. (http://code.google.com/p/disruptor/wiki/FrequentlyAskedQuestions)
        if ((sequence % numberOfConsumers) != ordinal) {
            return;
        }

        outputBufferWatermark.decrementAndGet();
        incomingMessages.mark();

        final Message msg = event.getMessage();
        LOG.debug("Processing message <{}> from OutputBuffer.", msg.getId());

        final Set<MessageOutput> messageOutputs = outputRouter.getOutputsForMessage(msg);
        final CountDownLatch doneSignal = new CountDownLatch(messageOutputs.size());
        for (final MessageOutput output : messageOutputs) {
            if (output == null) {
                LOG.error("Got null output!");
                continue;
            }
            try {
                LOG.debug("Writing message to [{}].", output.getName());
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Message id for [{}]: <{}>", output.getName(), msg.getId());
                }
                executor.submit(new Runnable() {
                    @Override
                    public void run() {
                        try(Timer.Context context = processTime.time()) {
                            output.write(msg);
                        } catch (Exception e) {
                            LOG.error("Error in output [" + output.getName() +"].", e);
                        } finally {
                            doneSignal.countDown();
                        }
                    }
                });

            } catch (Exception e) {
                LOG.error("Could not write message batch to output [" + output.getName() +"].", e);
                doneSignal.countDown();
            }
        }

        // Wait until all writer threads have finished or timeout is reached.
        if (!doneSignal.await(configuration.getOutputModuleTimeout(), TimeUnit.MILLISECONDS)) {
            LOG.warn("Timeout reached. Not waiting any longer for writer threads to complete.");
        }

        if (serverStatus.hasCapability(ServerStatus.Capability.STATSMODE)) {
            throughputStats.getBenchmarkCounter().increment();
        }

        throughputStats.getThroughputCounter().increment();

        LOG.debug("Wrote message <{}> to all outputs. Finished handling.", msg.getId());
    }
}

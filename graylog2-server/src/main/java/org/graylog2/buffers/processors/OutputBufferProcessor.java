/**
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
import com.codahale.metrics.InstrumentedExecutorService;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.lmax.disruptor.WorkHandler;
import org.graylog2.Configuration;
import org.graylog2.outputs.CachedOutputRouter;
import org.graylog2.outputs.OutputRouter;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.buffers.MessageEvent;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.shared.stats.ThroughputStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.codahale.metrics.MetricRegistry.name;

public class OutputBufferProcessor implements WorkHandler<MessageEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(OutputBufferProcessor.class);

    private final ExecutorService executor;

    private final Configuration configuration;
    private final ThroughputStats throughputStats;
    private final ServerStatus serverStatus;

    //private List<Message> buffer = Lists.newArrayList();

    private final Meter incomingMessages;
    private final Histogram batchSize;
    private final Timer processTime;

    private final OutputRouter outputRouter;

    @Inject
    public OutputBufferProcessor(Configuration configuration,
                                 MetricRegistry metricRegistry,
                                 ThroughputStats throughputStats,
                                 ServerStatus serverStatus,
                                 CachedOutputRouter outputRouter) {
        this.configuration = configuration;
        this.throughputStats = throughputStats;
        this.serverStatus = serverStatus;
        this.outputRouter = outputRouter;

        final String nameFormat = "outputbuffer-processor-executor-%d";
        final int corePoolSize = configuration.getOutputBufferProcessorThreadsCorePoolSize();
        final int maxPoolSize = configuration.getOutputBufferProcessorThreadsMaxPoolSize();
        final int keepAliveTime = configuration.getOutputBufferProcessorKeepAliveTime();
        this.executor = executorService(metricRegistry, nameFormat, corePoolSize, maxPoolSize, keepAliveTime);

        this.incomingMessages = metricRegistry.meter(name(OutputBufferProcessor.class, "incomingMessages"));
        this.batchSize = metricRegistry.histogram(name(OutputBufferProcessor.class, "batchSize"));
        this.processTime = metricRegistry.timer(name(OutputBufferProcessor.class, "processTime"));
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

    @Override
    public void onEvent(MessageEvent event) throws Exception {
        incomingMessages.mark();

        final Message msg = event.getMessage();
        if (msg == null) {
            LOG.debug("Skipping null message.");
            return;
        }
        LOG.debug("Processing message <{}> from OutputBuffer.", msg.getId());

        final Set<MessageOutput> messageOutputs = outputRouter.getOutputsForMessage(msg);
        msg.recordCounter(serverStatus, "matched-outputs", messageOutputs.size());
        final CountDownLatch doneSignal = new CountDownLatch(messageOutputs.size());
        for (final MessageOutput output : messageOutputs) {
            if (output == null) {
                LOG.error("Got null output!");
                continue;
            }
            if (!output.isRunning()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Skipping stopped output {}", output.getClass().getName());
                }
                continue;
            }
            try {
                LOG.debug("Writing message to [{}].", output.getClass());
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Message id for [{}]: <{}>", output.getClass(), msg.getId());
                }
                executor.submit(new Runnable() {
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
        }

        // Wait until all writer threads have finished or timeout is reached.
        if (!doneSignal.await(configuration.getOutputModuleTimeout(), TimeUnit.MILLISECONDS)) {
            LOG.warn("Timeout reached. Not waiting any longer for writer threads to complete.");
        }

        if (msg.hasRecordings()) {
            LOG.debug("Message event trace: {}", msg.recordingsAsString());
        }
        if (serverStatus.hasCapability(ServerStatus.Capability.STATSMODE)) {
            throughputStats.getBenchmarkCounter().increment();
        }

        throughputStats.getThroughputCounter().increment();

        LOG.debug("Wrote message <{}> to all outputs. Finished handling.", msg.getId());
    }
}

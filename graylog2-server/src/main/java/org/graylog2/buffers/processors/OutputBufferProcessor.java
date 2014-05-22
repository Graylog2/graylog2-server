/**
 * Copyright 2012 Lennart Koopmann <lennart@socketfeed.com>
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
 *
 */

package org.graylog2.buffers.processors;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.lmax.disruptor.EventHandler;
import org.bson.types.ObjectId;
import org.graylog2.Core;
import org.graylog2.outputs.OutputRouter;
import org.graylog2.outputs.OutputStreamConfigurationImpl;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.buffers.MessageEvent;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.plugin.outputs.OutputStreamConfiguration;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.streams.StreamImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class OutputBufferProcessor implements EventHandler<MessageEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(OutputBufferProcessor.class);

    private final ExecutorService executor;

    private Core server;

    private final Meter incomingMessages;
    private final Timer processTime;

    private final long ordinal;
    private final long numberOfConsumers;

    public OutputBufferProcessor(Core server, final long ordinal, final long numberOfConsumers) {
        this.ordinal = ordinal;
        this.numberOfConsumers = numberOfConsumers;
        this.server = server;

        executor = new ThreadPoolExecutor(
            server.getConfiguration().getOutputBufferProcessorThreadsCorePoolSize(),
            server.getConfiguration().getOutputBufferProcessorThreadsMaxPoolSize(),
            5, TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>(),
            new ThreadFactoryBuilder()
            .setNameFormat("outputbuffer-processor-" + ordinal + "-executor-%d")
            .build());

        incomingMessages = server.metrics().meter(name(OutputBufferProcessor.class, "incomingMessages"));
        processTime = server.metrics().timer(name(OutputBufferProcessor.class, "processTime"));
    }

    @Override
    public void onEvent(MessageEvent event, long sequence, boolean endOfBatch) throws Exception {
        // Because Trisha said so. (http://code.google.com/p/disruptor/wiki/FrequentlyAskedQuestions)
        if ((sequence % numberOfConsumers) != ordinal) {
            return;
        }

        server.outputBufferWatermark().decrementAndGet();
        incomingMessages.mark();

        Message msg = event.getMessage();
        LOG.debug("Processing message <{}> from OutputBuffer.", msg.getId());

        final List<Message> msgBuffer = Lists.newArrayList();
        msgBuffer.add(msg);

        final CountDownLatch doneSignal = new CountDownLatch(server.outputs().count());
        for (final MessageOutput output : server.outputs().get()) {
            final String typeClass = output.getClass().getCanonicalName();

            try {
                LOG.debug("Writing message batch to [{}]. Size <{}>", output.getName(), msgBuffer.size());
                if (LOG.isTraceEnabled()) {
                    final List<String> sortedIds = Ordering.natural().sortedCopy(Lists.transform(msgBuffer, Message.ID_FUNCTION));
                    LOG.trace("Message ids in batch of [{}]: <{}>", output.getName(), Joiner.on(", ").join(sortedIds));
                }
                executor.submit(new Runnable() {
                    @Override
                    public void run() {
                        try(Timer.Context context = processTime.time()) {
                            output.write(
                                    OutputRouter.getMessagesForOutput(msgBuffer, typeClass),
                                    buildStreamConfigs(msgBuffer, typeClass),
                                    server
                            );
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
        if (!doneSignal.await(10, TimeUnit.SECONDS)) {
            LOG.warn("Timeout reached. Not waiting any longer for writer threads to complete.");
        }

        int messagesWritten = msgBuffer.size();

        if (server.isStatsMode()) {
            server.getBenchmarkCounter().add(messagesWritten);
        }

        server.getThroughputCounter().add(messagesWritten);

        msgBuffer.clear();

        LOG.debug("Wrote message <{}> to all outputs. Finished handling.", msg.getId());
    }

    private OutputStreamConfiguration buildStreamConfigs(List<Message> messages, String className) {
        OutputStreamConfiguration configs = new OutputStreamConfigurationImpl();
        Map<ObjectId, Stream> distinctStreams = Maps.newHashMap();

        for (Message message : messages) {
            for (Stream stream : message.getStreams()) {
                distinctStreams.put(new ObjectId(stream.getId()), stream);
            }
        }

        for (Map.Entry<ObjectId, Stream> e : distinctStreams.entrySet()) {
            StreamImpl stream = (StreamImpl) e.getValue();
            configs.add(e.getKey(), stream.getOutputConfigurations(className));
        }

        return configs;
    }

}

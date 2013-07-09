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

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import org.bson.types.ObjectId;
import com.google.common.collect.Maps;
import org.graylog2.Core;
import org.graylog2.plugin.buffers.MessageEvent;
import org.graylog2.outputs.OutputRouter;
import org.graylog2.outputs.OutputStreamConfigurationImpl;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.plugin.outputs.OutputStreamConfiguration;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.streams.StreamImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.lmax.disruptor.EventHandler;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class OutputBufferProcessor implements EventHandler<MessageEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(OutputBufferProcessor.class);

    private final ExecutorService executor;
    
    private Core server;

    private List<Message> buffer = Lists.newArrayList();

    private final Meter incomingMessages;
    private final Histogram batchSize;

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
        batchSize = server.metrics().histogram(name(OutputBufferProcessor.class, "batchSize"));
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

        buffer.add(msg);

        if (endOfBatch || buffer.size() >= server.getConfiguration().getOutputBatchSize()) {

            final CountDownLatch doneSignal = new CountDownLatch(server.outputs().count());
            for (final MessageOutput output : server.outputs().get()) {
                final String typeClass = output.getClass().getCanonicalName();

                try {
                    // We must copy the buffer for this output, because it may be cleared before all messages are handled.
                    final List<Message> myBuffer = Lists.newArrayList(buffer);

                    LOG.debug("Writing message batch to [{}]. Size <{}>", output.getName(), buffer.size());

                    batchSize.update(buffer.size());

                    executor.submit(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                output.write(
                                        OutputRouter.getMessagesForOutput(myBuffer, typeClass),
                                        buildStreamConfigs(myBuffer, typeClass),
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

            int messagesWritten = buffer.size();

            if (server.isStatsMode()) {
                server.getBenchmarkCounter().add(messagesWritten);
            }

            server.getThroughputCounter().add(messagesWritten);
            
            buffer.clear();
        }

        LOG.debug("Wrote message <{}> to all outputs. Finished handling.", msg.getId());
    }

    private OutputStreamConfiguration buildStreamConfigs(List<Message> messages, String className) {
        OutputStreamConfiguration configs = new OutputStreamConfigurationImpl();
        Map<ObjectId, Stream> distinctStreams = Maps.newHashMap();
        
        for (Message message : messages) {
            for (Stream stream : message.getStreams()) {
                distinctStreams.put(stream.getId(), stream);
            }
        }
        
        for (Map.Entry<ObjectId, Stream> e : distinctStreams.entrySet()) {
            StreamImpl stream = (StreamImpl) e.getValue();
            configs.add(e.getKey(), stream.getOutputConfigurations(className));
        }
        
        return configs;
    }
    
}

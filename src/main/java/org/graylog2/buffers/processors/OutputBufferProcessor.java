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

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.lmax.disruptor.EventHandler;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Histogram;
import com.yammer.metrics.core.Meter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.graylog2.Core;
import org.graylog2.buffers.LogMessageEvent;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.plugin.logmessage.LogMessage;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.bson.types.ObjectId;
import org.elasticsearch.common.collect.Maps;
import org.graylog2.outputs.ElasticSearchOutput;
import org.graylog2.outputs.OutputRouter;
import org.graylog2.outputs.OutputStreamConfigurationImpl;
import org.graylog2.plugin.outputs.OutputStreamConfiguration;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.streams.StreamImpl;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class OutputBufferProcessor implements EventHandler<LogMessageEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(OutputBufferProcessor.class);

    private final ExecutorService executor;
    
    private Core server;

    private List<LogMessage> buffer = Lists.newArrayList();
    private final Meter incomingMessages = Metrics.newMeter(OutputBufferProcessor.class, "IncomingMessages", "messages", TimeUnit.SECONDS);
    
    private final Histogram batchSize = Metrics.newHistogram(OutputBufferProcessor.class, "BatchSize");

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
    }

    @Override
    public void onEvent(LogMessageEvent event, long sequence, boolean endOfBatch) throws Exception {
        // Because Trisha said so. (http://code.google.com/p/disruptor/wiki/FrequentlyAskedQuestions)
        if ((sequence % numberOfConsumers) != ordinal) {
            return;
        }
        
        server.outputBufferWatermark().decrementAndGet();
        incomingMessages.mark();

        LogMessage msg = event.getMessage();
        LOG.debug("Processing message <{}> from OutputBuffer.", msg.getId());

        buffer.add(msg);

        if (endOfBatch || buffer.size() >= server.getConfiguration().getOutputBatchSize()) {
            for (final MessageOutput output : server.getOutputs()) {
                final String typeClass = output.getClass().getCanonicalName();
                // Always write to ElasticSearch, but only write to other outputs if enabled for one of its streams.
                if (output instanceof ElasticSearchOutput || OutputRouter.checkRouting(typeClass, msg)) {
                    try {
                        LOG.debug("Writing message batch to [{}]. Size <{}>", output.getName(), buffer.size());

                        batchSize.update(buffer.size());
                        
                        executor.submit(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    output.write(buffer, buildStreamConfigs(buffer, typeClass), server);
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        });
                        
                    } catch (Exception e) {
                        LOG.error("Could not write message batch to output [" + output.getName() +"].", e);
                    }
                }
            }
            
            buffer.clear();
        }

        LOG.debug("Wrote message <{}> to all outputs. Finished handling.", msg.getId());
    }

    private OutputStreamConfiguration buildStreamConfigs(List<LogMessage> messages, String className) {
        OutputStreamConfiguration configs = new OutputStreamConfigurationImpl();
        Map<ObjectId, Stream> distinctStreams = Maps.newHashMap();
        
        for (LogMessage message : messages) {
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

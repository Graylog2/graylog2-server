package org.graylog2.buffers.processors;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.buffers.OutputBuffer;
import org.graylog2.plugin.GraylogServer;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.filters.MessageFilter;
import org.graylog2.shared.buffers.processors.ProcessBufferProcessor;
import org.graylog2.shared.filters.FilterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class ServerProcessBufferProcessor extends ProcessBufferProcessor {
    public interface Factory {
        public ServerProcessBufferProcessor create(
                GraylogServer server,
                OutputBuffer outputBuffer,
                @Assisted("ordinal") final long ordinal,
                @Assisted("numberOfConsumers") final long numberOfConsumers
        );
    }

    private static final Logger LOG = LoggerFactory.getLogger(ServerProcessBufferProcessor.class);
    private final GraylogServer server;
    private final OutputBuffer outputBuffer;
    private final Meter filteredOutMessages;
    private final FilterRegistry filterRegistry;


    @AssistedInject
    public ServerProcessBufferProcessor(MetricRegistry metricRegistry,
                                  FilterRegistry filterRegistry,
                                  @Assisted GraylogServer server,
                                  @Assisted("ordinal") final long ordinal,
                                  @Assisted("numberOfConsumers") final long numberOfConsumers,
                                  @Assisted OutputBuffer outputBuffer) {
        super(metricRegistry, server.processBufferWatermark(), ordinal, numberOfConsumers);
        this.filterRegistry = filterRegistry;
        this.server = server;
        this.outputBuffer = outputBuffer;
        this.filteredOutMessages = metricRegistry.meter(name(ProcessBufferProcessor.class, "filteredOutMessages"));
    }

    @Override
    protected void handleMessage(Message msg) {

        for (MessageFilter filter : filterRegistry.all()) {
            Timer timer = metricRegistry.timer(name(filter.getClass(), "executionTime"));
            final Timer.Context timerContext = timer.time();

            try {
                LOG.debug("Applying filter [{}] on message <{}>.", filter.getName(), msg.getId());

                if (filter.filter(msg, server)) {
                    LOG.debug("Filter [{}] marked message <{}> to be discarded. Dropping message.", filter.getName(), msg.getId());
                    filteredOutMessages.mark();
                    return;
                }
            } catch (Exception e) {
                LOG.error("Could not apply filter [" + filter.getName() +"] on message <" + msg.getId() +">: ", e);
            } finally {
                timerContext.stop();
            }
        }

        LOG.debug("Finished processing message. Writing to output buffer.");
        outputBuffer.insertCached(msg, null);
    }
}

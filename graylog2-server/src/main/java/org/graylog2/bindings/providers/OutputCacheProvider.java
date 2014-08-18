package org.graylog2.bindings.providers;

import com.codahale.metrics.MetricRegistry;
import org.graylog2.Configuration;
import org.graylog2.caches.DiskJournalCache;
import org.graylog2.caches.DiskJournalCacheCorruptSpoolException;
import org.graylog2.inputs.OutputCache;
import org.graylog2.utilities.MessageToJsonSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import java.io.IOException;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class OutputCacheProvider implements Provider<OutputCache> {
    private static final Logger LOG = LoggerFactory.getLogger(DiskJournalCache.class);
    private final Configuration configuration;
    private final MessageToJsonSerializer messageToJsonSerializer;
    private final MetricRegistry metricRegistry;

    @Inject
    public OutputCacheProvider(Configuration configuration, MessageToJsonSerializer messageToJsonSerializer, MetricRegistry metricRegistry) {
        this.configuration = configuration;
        this.messageToJsonSerializer = messageToJsonSerializer;
        this.metricRegistry = metricRegistry;
    }

    @Override
    public OutputCache get() {
        try {
            return new DiskJournalCache.Output(configuration, messageToJsonSerializer, metricRegistry);
        } catch (IOException e) {
            LOG.error("Unable to create spool directory {}: {}", configuration.getMessageCacheSpoolDir(), e);
            System.exit(-1);
        } catch (DiskJournalCacheCorruptSpoolException e) {
            LOG.error("Unable to initialize output journal spool.");
            LOG.error("This means that your spool files (in directory \"{}\") got corrupted. Please repair or remove them.", configuration.getMessageCacheSpoolDir());
            System.exit(-1);
        }
        return null;
    }
}

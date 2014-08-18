package org.graylog2.bindings.providers;

import com.codahale.metrics.MetricRegistry;
import org.graylog2.Configuration;
import org.graylog2.caches.DiskJournalCache;
import org.graylog2.caches.DiskJournalCacheCorruptSpoolException;
import org.graylog2.inputs.InputCache;
import org.graylog2.utilities.MessageToJsonSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import java.io.IOException;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class InputCacheProvider implements Provider<InputCache> {
    private static final Logger LOG = LoggerFactory.getLogger(DiskJournalCache.class);
    private final MetricRegistry metricRegistry;
    private final MessageToJsonSerializer messageToJsonSerializer;
    private final Configuration configuration;

    @Inject
    public InputCacheProvider(MetricRegistry metricRegistry, MessageToJsonSerializer messageToJsonSerializer, Configuration configuration) {
        this.metricRegistry = metricRegistry;
        this.messageToJsonSerializer = messageToJsonSerializer;
        this.configuration = configuration;
    }

    @Override
    public InputCache get() {
        try {
            return new DiskJournalCache.Input(configuration, messageToJsonSerializer, metricRegistry);
        } catch (IOException e) {
            LOG.error("Unable to create spool directory {}: {}", configuration.getMessageCacheSpoolDir(), e);
            System.exit(-1);
        } catch (DiskJournalCacheCorruptSpoolException e) {
            LOG.error("Unable to initialize input journal spool.");
            LOG.error("This means that your spool files (in directory \"{}\") got corrupted. Please repair or remove them.", configuration.getMessageCacheSpoolDir());
            System.exit(-1);
        }
        return null;
    }
}

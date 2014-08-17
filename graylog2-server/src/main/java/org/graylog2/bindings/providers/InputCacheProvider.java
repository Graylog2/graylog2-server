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

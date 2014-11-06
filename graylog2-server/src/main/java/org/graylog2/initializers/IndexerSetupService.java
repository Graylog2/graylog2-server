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
package org.graylog2.initializers;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Singleton;
import org.elasticsearch.Version;
import org.graylog2.indexer.Deflector;
import org.graylog2.indexer.Indexer;
import org.graylog2.plugin.Tools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.concurrent.Executors;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
@Singleton
public class IndexerSetupService extends AbstractIdleService {
    private static final Logger LOG = LoggerFactory.getLogger(IndexerSetupService.class);
    private static final Version MINIMUM_ES_VERSION = Version.V_1_3_4;
    private static final Version MAXIMUM_ES_VERSION = Version.fromString("1.4.99");

    private final Indexer indexer;
    private final Deflector deflector;
    private final BufferSynchronizerService bufferSynchronizerService;

    @Inject
    public IndexerSetupService(final Indexer indexer,
                               final Deflector deflector,
                               final BufferSynchronizerService bufferSynchronizerService) {
        this.indexer = indexer;
        this.deflector = deflector;
        this.bufferSynchronizerService = bufferSynchronizerService;

        // Shutdown after the BufferSynchronizerServer has stopped to avoid shutting down ES too early.
        bufferSynchronizerService.addListener(new Listener() {
            @Override
            public void terminated(State from) {
                LOG.debug("Shutting down ES client after buffer synchronizer has terminated.");
                // Properly close ElasticSearch node.
                IndexerSetupService.this.indexer.getNode().close();
            }
        }, Executors.newSingleThreadExecutor());
    }

    @Override
    protected void startUp() throws Exception {
        Tools.silenceUncaughtExceptionsInThisThread();

        LOG.debug("Starting indexer");
        try {
            indexer.start();
        } catch (Exception e) {
            bufferSynchronizerService.setIndexerUnavailable();
            throw e;
        }

        LOG.debug("Setting up deflector");
        deflector.setUp(indexer);
    }

    @Override
    protected void shutDown() throws Exception {
        // See constructor. Actual shutdown happens after BufferSynchronizerServer has stopped.
    }
}

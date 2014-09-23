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
import org.graylog2.buffers.Buffers;
import org.graylog2.caches.Caches;
import org.graylog2.indexer.cluster.Cluster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class BufferSynchronizerService extends AbstractIdleService {
    private static final Logger LOG = LoggerFactory.getLogger(BufferSynchronizerService.class);

    private final Buffers bufferSynchronizer;
    private final Caches cacheSynchronizer;
    private final Cluster cluster;

    private volatile boolean indexerAvailable = true;

    @Inject
    public BufferSynchronizerService(final Buffers bufferSynchronizer,
                                     final Caches cacheSynchronizer,
                                     final Cluster cluster) {
        this.bufferSynchronizer = bufferSynchronizer;
        this.cacheSynchronizer = cacheSynchronizer;
        this.cluster = cluster;
    }

    @Override
    protected void startUp() throws Exception {
    }

    @Override
    protected void shutDown() throws Exception {
        LOG.debug("Stopping BufferSynchronizerService");
        if (indexerAvailable && cluster.isConnectedAndHealthy()) {
            bufferSynchronizer.waitForEmptyBuffers();
            cacheSynchronizer.waitForEmptyCaches();
        } else {
            LOG.warn("Elasticsearch is unavailable. Not waiting to clear buffers and caches, as we have no healthy cluster.");
        }
        LOG.debug("Stopped BufferSynchronizerService");
    }

    public void setIndexerUnavailable() {
        indexerAvailable = false;
    }
}

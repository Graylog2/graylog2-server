/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.indexer.fieldtypes;

import com.github.joschi.jadconfig.util.Duration;
import com.google.common.eventbus.EventBus;
import org.graylog2.indexer.MongoIndexSet;
import org.graylog2.indexer.cluster.Cluster;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.lifecycles.Lifecycle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ScheduledExecutorService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IndexFieldTypePollerPeriodicalTest {
    private IndexFieldTypePollerPeriodical periodical;
    private final IndexFieldTypePoller indexFieldTypePoller = mock(IndexFieldTypePoller.class);
    private final IndexFieldTypesService indexFieldTypesService = mock(IndexFieldTypesService.class);
    private final IndexSetService indexSetService = mock(IndexSetService.class);
    private final Indices indices = mock(Indices.class);
    private final MongoIndexSet.Factory mongoIndexSetFactory = mock(MongoIndexSet.Factory.class);
    private final Cluster cluster = mock(Cluster.class);
    @SuppressWarnings("UnstableApiUsage")
    private final EventBus eventBus = mock(EventBus.class);
    private final ServerStatus serverStatus = mock(ServerStatus.class);
    private final ScheduledExecutorService scheduler = mock(ScheduledExecutorService.class);

    @BeforeEach
    void setUp() {
        this.periodical = new IndexFieldTypePollerPeriodical(indexFieldTypePoller,
                indexFieldTypesService,
                indexSetService,
                indices,
                mongoIndexSetFactory,
                cluster,
                eventBus,
                serverStatus,
                Duration.seconds(30),
                scheduler);
    }

    @Test
    void executionIsSkippedWhenServerIsNotRunning() {
        when(serverStatus.getLifecycle()).thenReturn(Lifecycle.HALTING);
        when(cluster.isConnected()).thenThrow(new RuntimeException("If this exception is thrown, then execution is not skipped!"));

        this.periodical.doRun();
    }
}

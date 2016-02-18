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

package org.graylog2.indexer.rotation.strategies;

import org.elasticsearch.action.admin.indices.stats.CommonStats;
import org.elasticsearch.cluster.routing.ShardRouting;
import org.elasticsearch.index.store.StoreStats;
import org.graylog2.indexer.Deflector;
import org.graylog2.indexer.indices.IndexStatistics;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.Optional;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SizeBasedRotationStrategyTest {
    @Mock
    private ClusterConfigService clusterConfigService;

    @Mock
    private Deflector deflector;

    @Mock
    private Indices indices;

    @Test
    public void testRotate() throws Exception {
        final CommonStats commonStats = new CommonStats();
        commonStats.store = new StoreStats(1000, 0);
        final IndexStatistics stats = IndexStatistics.create("name", commonStats, commonStats, Collections.<ShardRouting>emptyList());

        when(indices.getIndexStats("name")).thenReturn(stats);
        when(deflector.getNewestTargetName()).thenReturn("name");
        when(clusterConfigService.get(SizeBasedRotationStrategyConfig.class)).thenReturn(Optional.of(SizeBasedRotationStrategyConfig.create(100L)));

        final SizeBasedRotationStrategy strategy = new SizeBasedRotationStrategy(indices, deflector, clusterConfigService);

        strategy.rotate();
        verify(deflector, times(1)).cycle();
        reset(deflector);
    }


    @Test
    public void testDontRotate() throws Exception {
        final CommonStats commonStats = new CommonStats();
        commonStats.store = new StoreStats(1000, 0);
        final IndexStatistics stats = IndexStatistics.create("name", commonStats, commonStats, Collections.<ShardRouting>emptyList());

        when(indices.getIndexStats("name")).thenReturn(stats);
        when(deflector.getNewestTargetName()).thenReturn("name");
        when(clusterConfigService.get(SizeBasedRotationStrategyConfig.class)).thenReturn(Optional.of(SizeBasedRotationStrategyConfig.create(100000L)));

        final SizeBasedRotationStrategy strategy = new SizeBasedRotationStrategy(indices, deflector, clusterConfigService);

        strategy.rotate();
        verify(deflector, never()).cycle();
        reset(deflector);
    }


    @Test
    public void testRotateFailed() throws Exception {
        when(indices.getIndexStats("name")).thenReturn(null);
        when(deflector.getNewestTargetName()).thenReturn("name");
        when(clusterConfigService.get(SizeBasedRotationStrategyConfig.class)).thenReturn(Optional.of(SizeBasedRotationStrategyConfig.create(100)));

        final SizeBasedRotationStrategy strategy = new SizeBasedRotationStrategy(indices, deflector, clusterConfigService);

        strategy.rotate();
        verify(deflector, never()).cycle();
        reset(deflector);
    }
}
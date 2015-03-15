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
package org.graylog2.indexer.rotation;

import org.elasticsearch.action.admin.indices.stats.CommonStats;
import org.elasticsearch.index.store.StoreStats;
import org.graylog2.configuration.ElasticsearchConfiguration;
import org.graylog2.indexer.IndexNotFoundException;
import org.graylog2.indexer.indices.IndexStatistics;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.plugin.indexer.rotation.RotationStrategy;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SizeBasedRotationStrategyTest {
    @Mock
    private ElasticsearchConfiguration configuration;
    @Mock
    private Indices indices;

    @Test
    public void testRotate() throws IndexNotFoundException {
        final IndexStatistics stats = new IndexStatistics();
        final CommonStats commonStats = new CommonStats();
        commonStats.store = new StoreStats(1000, 0);
        stats.setPrimaries(commonStats);

        when(indices.getIndexStats("name")).thenReturn(stats);
        when(configuration.getMaxSizePerIndex()).thenReturn(100L);

        final SizeBasedRotationStrategy strategy = new SizeBasedRotationStrategy(configuration, indices);
        final RotationStrategy.Result rotate = strategy.shouldRotate("name");

        verify(configuration, atLeastOnce()).getMaxSizePerIndex();
        assertNotNull(rotate);
        assertEquals(true, rotate.shouldRotate());
    }


    @Test
    public void testDontRotate() throws IndexNotFoundException {
        final IndexStatistics stats = new IndexStatistics();
        final CommonStats commonStats = new CommonStats();
        commonStats.store = new StoreStats(1000, 0);
        stats.setPrimaries(commonStats);

        when(indices.getIndexStats("name")).thenReturn(stats);
        when(configuration.getMaxSizePerIndex()).thenReturn(100000L);

        final SizeBasedRotationStrategy strategy = new SizeBasedRotationStrategy(configuration, indices);
        final RotationStrategy.Result rotate = strategy.shouldRotate("name");

        verify(configuration, atLeastOnce()).getMaxSizePerIndex();
        assertNotNull(rotate);
        assertEquals(false, rotate.shouldRotate());
    }


    @Test
    public void testRotateFailed() throws IndexNotFoundException {
        when(indices.getIndexStats("name")).thenReturn(null);
        when(configuration.getMaxSizePerIndex()).thenReturn(100L);

        final SizeBasedRotationStrategy strategy = new SizeBasedRotationStrategy(configuration, indices);
        final RotationStrategy.Result rotate = strategy.shouldRotate("name");

        verify(configuration, atLeastOnce()).getMaxSizePerIndex();
        assertNull(rotate);
    }
}
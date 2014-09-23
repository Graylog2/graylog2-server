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
package org.graylog2.indexer.rotation;

import org.elasticsearch.action.admin.indices.stats.CommonStats;
import org.elasticsearch.index.store.StoreStats;
import org.graylog2.Configuration;
import org.graylog2.indexer.IndexNotFoundException;
import org.graylog2.indexer.indices.IndexStatistics;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.plugin.indexer.rotation.RotationStrategy;
import org.testng.annotations.Test;

import static org.mockito.Mockito.*;
import static org.testng.Assert.assertNull;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

public class SizeBasedRotationStrategyTest {

    @Test
    public void testRotate() throws IndexNotFoundException {
        final Configuration configuration = mock(Configuration.class);
        final Indices indices = mock(Indices.class);

        final IndexStatistics stats = new IndexStatistics();
        final CommonStats commonStats = new CommonStats();
        commonStats.store = new StoreStats(1000, 0);
        stats.setPrimaries(commonStats);

        when(indices.getIndexStats("name")).thenReturn(stats);
        when(configuration.getElasticSearchMaxSizePerIndex()).thenReturn(100L);

        final SizeBasedRotationStrategy strategy = new SizeBasedRotationStrategy(configuration,
                                                                                       indices);
        final RotationStrategy.Result rotate = strategy.shouldRotate("name");

        verify(configuration, atLeastOnce()).getElasticSearchMaxSizePerIndex();
        assertNotNull(rotate);
        assertEquals(true, rotate.shouldRotate());
    }


    @Test
    public void testDontRotate() throws IndexNotFoundException {
        final Configuration configuration = mock(Configuration.class);
        final Indices indices = mock(Indices.class);

        final IndexStatistics stats = new IndexStatistics();
        final CommonStats commonStats = new CommonStats();
        commonStats.store = new StoreStats(1000, 0);
        stats.setPrimaries(commonStats);

        when(indices.getIndexStats("name")).thenReturn(stats);
        when(configuration.getElasticSearchMaxSizePerIndex()).thenReturn(100000L);

        final SizeBasedRotationStrategy strategy = new SizeBasedRotationStrategy(configuration,
                                                                                 indices);
        final RotationStrategy.Result rotate = strategy.shouldRotate("name");

        verify(configuration, atLeastOnce()).getElasticSearchMaxSizePerIndex();
        assertNotNull(rotate);
        assertEquals(false, rotate.shouldRotate());
    }


    @Test
    public void testRotateFailed() throws IndexNotFoundException {
        final Configuration configuration = mock(Configuration.class);
        final Indices indices = mock(Indices.class);

        when(indices.getIndexStats("name")).thenReturn(null);
        when(configuration.getElasticSearchMaxSizePerIndex()).thenReturn(100L);

        final SizeBasedRotationStrategy strategy = new SizeBasedRotationStrategy(configuration,
                                                                                 indices);
        final RotationStrategy.Result rotate = strategy.shouldRotate("name");

        verify(configuration, atLeastOnce()).getElasticSearchMaxSizePerIndex();
        assertNull(rotate);
    }

}
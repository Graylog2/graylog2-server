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

import org.graylog2.configuration.ElasticsearchConfiguration;
import org.graylog2.indexer.IndexNotFoundException;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.plugin.indexer.rotation.RotationStrategy;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MessageCountRotationStrategyTest {
    @Mock
    private ElasticsearchConfiguration configuration = mock(ElasticsearchConfiguration.class);
    @Mock
    private Indices indices = mock(Indices.class);

    @Test
    public void testRotate() throws IndexNotFoundException {
        when(indices.numberOfMessages("name")).thenReturn(10L);
        when(configuration.getMaxDocsPerIndex()).thenReturn(5);

        final MessageCountRotationStrategy strategy = new MessageCountRotationStrategy(configuration,
                indices);
        final RotationStrategy.Result rotate = strategy.shouldRotate("name");

        assertNotNull(rotate);
        assertEquals(true, rotate.shouldRotate());
    }

    @Test
    public void testDontRotate() throws IndexNotFoundException {
        when(indices.numberOfMessages("name")).thenReturn(1L);
        when(configuration.getMaxDocsPerIndex()).thenReturn(5);

        final MessageCountRotationStrategy strategy = new MessageCountRotationStrategy(configuration,
                indices);

        final RotationStrategy.Result rotate = strategy.shouldRotate("name");

        assertNotNull(rotate);
        assertEquals(false, rotate.shouldRotate());
    }


    @Test
    public void testIndexUnavailable() throws IndexNotFoundException {
        doThrow(IndexNotFoundException.class).when(indices).numberOfMessages("name");
        when(configuration.getMaxDocsPerIndex()).thenReturn(5);

        final MessageCountRotationStrategy strategy = new MessageCountRotationStrategy(configuration,
                indices);
        final RotationStrategy.Result rotate = strategy.shouldRotate("name");

        assertNull(rotate);
    }
}

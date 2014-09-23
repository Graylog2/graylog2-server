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

import org.graylog2.Configuration;
import org.graylog2.indexer.IndexNotFoundException;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.plugin.indexer.rotation.RotationStrategy;
import org.testng.annotations.Test;

import static org.mockito.Mockito.*;
import static org.testng.Assert.assertNull;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

public class MessageCountRotationStrategyTest {

    @Test
    public void testRotate() throws IndexNotFoundException {
        final Configuration configuration = mock(Configuration.class);
        final Indices indices = mock(Indices.class);

        when(indices.numberOfMessages("name")).thenReturn(10L);
        when(configuration.getElasticSearchMaxDocsPerIndex()).thenReturn(5);

        final MessageCountRotationStrategy strategy = new MessageCountRotationStrategy(configuration,
                                                                                       indices);
        final RotationStrategy.Result rotate = strategy.shouldRotate("name");

        assertNotNull(rotate);
        assertEquals(true, rotate.shouldRotate());
    }

    @Test
    public void testDontRotate() throws IndexNotFoundException {
        final Configuration configuration = mock(Configuration.class);
        final Indices indices = mock(Indices.class);

        when(indices.numberOfMessages("name")).thenReturn(1L);
        when(configuration.getElasticSearchMaxDocsPerIndex()).thenReturn(5);

        final MessageCountRotationStrategy strategy = new MessageCountRotationStrategy(configuration,
                                                                                       indices);

        final RotationStrategy.Result rotate = strategy.shouldRotate("name");

        assertNotNull(rotate);
        assertEquals(false, rotate.shouldRotate());
    }


    @Test
    public void testIndexUnavailable() throws IndexNotFoundException {
        final Configuration configuration = mock(Configuration.class);
        final Indices indices = mock(Indices.class);

        when(indices.numberOfMessages("name")).thenThrow(IndexNotFoundException.class).thenReturn(1L);
        when(configuration.getElasticSearchMaxDocsPerIndex()).thenReturn(5);

        final MessageCountRotationStrategy strategy = new MessageCountRotationStrategy(configuration,
                                                                                       indices);
        final RotationStrategy.Result rotate = strategy.shouldRotate("name");

        assertNull(rotate);
    }


}
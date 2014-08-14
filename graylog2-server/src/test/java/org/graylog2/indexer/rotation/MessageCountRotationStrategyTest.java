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
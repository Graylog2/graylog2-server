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
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.graylog2.indexer;

import org.elasticsearch.action.admin.indices.stats.IndexStats;
import org.graylog2.configuration.ElasticsearchConfiguration;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.ranges.CreateNewSingleIndexRangeJob;
import org.graylog2.system.activities.SystemMessageActivityWriter;
import org.graylog2.system.jobs.SystemJobManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class DeflectorTest {
    private Deflector deflector;

    @Before
    public void setUp() {
        deflector = new Deflector(
                mock(SystemJobManager.class),
                new ElasticsearchConfiguration(),
                mock(SystemMessageActivityWriter.class),
                mock(SetIndexReadOnlyJob.Factory.class),
                mock(CreateNewSingleIndexRangeJob.Factory.class),
                mock(Indices.class));
    }

    @Test
    public void testExtractIndexNumber() {
        assertEquals(0, Deflector.extractIndexNumber("graylog2_0"));
        assertEquals(4, Deflector.extractIndexNumber("graylog2_4"));
        assertEquals(52, Deflector.extractIndexNumber("graylog2_52"));
    }

    @Test
    public void testExtractIndexNumberWithCustomIndexPrefix() {
        assertEquals(0, Deflector.extractIndexNumber("foo_0_bar_0"));
        assertEquals(4, Deflector.extractIndexNumber("foo_0_bar_4"));
        assertEquals(52, Deflector.extractIndexNumber("foo_0_bar_52"));
    }

    @Test(expected = NumberFormatException.class)
    public void testExtractIndexNumberWithMalformedFormatThrowsException() {
        Deflector.extractIndexNumber("graylog2_hunderttausend");
    }

    @Test
    public void testBuildIndexName() {

        Deflector d = new Deflector(mock(SystemJobManager.class),
                mock(ElasticsearchConfiguration.class),
                mock(SystemMessageActivityWriter.class),
                mock(SetIndexReadOnlyJob.Factory.class),
                mock(CreateNewSingleIndexRangeJob.Factory.class),
                mock(Indices.class));

        assertEquals("graylog2_0", Deflector.buildIndexName("graylog2", 0));
        assertEquals("graylog2_1", Deflector.buildIndexName("graylog2", 1));
        assertEquals("graylog2_9001", Deflector.buildIndexName("graylog2", 9001));
    }

    @Test
    public void testBuildDeflectorNameWithCustomIndexPrefix() {
        assertEquals("foo_custom_index_deflector", "foo_custom_index" + "_" + Deflector.DEFLECTOR_SUFFIX);
    }

    @Test
    public void nullIndexerDoesNotThrow() {
        Deflector d = new Deflector(mock(SystemJobManager.class),
                mock(ElasticsearchConfiguration.class),
                mock(SystemMessageActivityWriter.class),
                mock(SetIndexReadOnlyJob.Factory.class),
                mock(CreateNewSingleIndexRangeJob.Factory.class),
                mock(Indices.class));

        final Map<String, IndexStats> deflectorIndices = d.getAllDeflectorIndices();
        assertNotNull(deflectorIndices);
        assertEquals(0, deflectorIndices.size());
    }

    @Test
    public void nullIndexerDoesNotThrowOnIndexName() {
        Deflector d = new Deflector(mock(SystemJobManager.class),
                mock(ElasticsearchConfiguration.class),
                mock(SystemMessageActivityWriter.class),
                mock(SetIndexReadOnlyJob.Factory.class),
                mock(CreateNewSingleIndexRangeJob.Factory.class),
                mock(Indices.class));
        final String[] deflectorIndices = d.getAllDeflectorIndexNames();
        assertNotNull(deflectorIndices);
        assertEquals(0, deflectorIndices.length);
    }

    @Test
    public void testIsDeflectorAlias() {
        assertTrue(deflector.isDeflectorAlias("graylog_deflector"));
        assertFalse(deflector.isDeflectorAlias("graylog_foobar"));
        assertFalse(deflector.isDeflectorAlias("graylog_123"));
        assertFalse(deflector.isDeflectorAlias("HAHA"));
    }

    @Test
    public void testIsGraylog2Index() {
        assertTrue(deflector.isGraylog2Index("graylog_1"));
        assertTrue(deflector.isGraylog2Index("graylog_42"));
        assertTrue(deflector.isGraylog2Index("graylog_100000000"));
        assertFalse(deflector.isGraylog2Index("graylog_deflector"));
        assertFalse(deflector.isGraylog2Index("graylog2beta_1"));
        assertFalse(deflector.isGraylog2Index("HAHA"));
    }
}

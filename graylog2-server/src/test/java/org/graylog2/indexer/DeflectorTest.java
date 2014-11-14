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
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.graylog2.indexer;

import org.elasticsearch.action.admin.indices.stats.IndexStats;
import org.graylog2.Configuration;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.indices.jobs.OptimizeIndexJob;
import org.graylog2.indexer.ranges.CreateNewSingleIndexRangeJob;
import org.graylog2.indexer.ranges.RebuildIndexRangesJob;
import org.graylog2.system.activities.SystemMessageActivityWriter;
import org.graylog2.system.jobs.SystemJobManager;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.testng.Assert.*;
import static org.testng.AssertJUnit.assertEquals;

public class DeflectorTest {

    private Deflector deflector;

    @BeforeMethod
    public void setUp() {
        deflector = new Deflector(
                mock(SystemJobManager.class),
                new Configuration(),
                mock(SystemMessageActivityWriter.class),
                mock(RebuildIndexRangesJob.Factory.class),
                mock(OptimizeIndexJob.Factory.class),
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

    @Test(expectedExceptions = NumberFormatException.class)
    public void testExtractIndexNumberWithMalformedFormatThrowsException() {
        Deflector.extractIndexNumber("graylog2_hunderttausend");
    }

    @Test
    public void testBuildIndexName() {

        Deflector d = new Deflector(mock(SystemJobManager.class),
                mock(Configuration.class),
                mock(SystemMessageActivityWriter.class),
                mock(RebuildIndexRangesJob.Factory.class),
                mock(OptimizeIndexJob.Factory.class),
                mock(CreateNewSingleIndexRangeJob.Factory.class),
                mock(Indices.class));

        assertEquals("graylog2_0", d.buildIndexName("graylog2", 0));
        assertEquals("graylog2_1", d.buildIndexName("graylog2", 1));
        assertEquals("graylog2_9001", d.buildIndexName("graylog2", 9001));
    }

    @Test
    public void testBuildDeflectorNameWithCustomIndexPrefix() {
        assertEquals("foo_custom_index_deflector", "foo_custom_index" + "_" + Deflector.DEFLECTOR_SUFFIX);
    }

    @Test
    public void nullIndexerDoesNotThrow() {
        Deflector d = new Deflector(mock(SystemJobManager.class),
                                    mock(Configuration.class),
                                    mock(SystemMessageActivityWriter.class),
                                    mock(RebuildIndexRangesJob.Factory.class),
                                    mock(OptimizeIndexJob.Factory.class),
                                    mock(CreateNewSingleIndexRangeJob.Factory.class),
                                    mock(Indices.class));
        try {
            final Map<String, IndexStats> deflectorIndices = d.getAllDeflectorIndices();
            assertNotNull(deflectorIndices);
            assertEquals(0, deflectorIndices.size());
        } catch (Exception e) {
            fail("Should not throw an exception", e);
        }
    }

    @Test
    public void nullIndexerDoesNotThrowOnIndexName() {
        Deflector d = new Deflector(mock(SystemJobManager.class),
                                    mock(Configuration.class),
                                    mock(SystemMessageActivityWriter.class),
                                    mock(RebuildIndexRangesJob.Factory.class),
                                    mock(OptimizeIndexJob.Factory.class),
                                    mock(CreateNewSingleIndexRangeJob.Factory.class),
                                    mock(Indices.class));
        try {
            final String[] deflectorIndices = d.getAllDeflectorIndexNames();
            assertNotNull(deflectorIndices);
            assertEquals(0, deflectorIndices.length);
        } catch (Exception e) {
            fail("Should not throw an exception", e);
        }
    }

    @Test
    public void testIsDeflectorAlias() {
        assertTrue(deflector.isDeflectorAlias("graylog2_deflector"));
        assertFalse(deflector.isDeflectorAlias("graylog2_foobar"));
        assertFalse(deflector.isDeflectorAlias("graylog2_123"));
        assertFalse(deflector.isDeflectorAlias("HAHA"));
    }

    @Test
    public void testIsGraylog2Index() {
        assertTrue(deflector.isGraylog2Index("graylog2_1"));
        assertTrue(deflector.isGraylog2Index("graylog2_42"));
        assertTrue(deflector.isGraylog2Index("graylog2_100000000"));
        assertFalse(deflector.isGraylog2Index("graylog2_deflector"));
        assertFalse(deflector.isGraylog2Index("graylog2beta_1"));
        assertFalse(deflector.isGraylog2Index("HAHA"));
    }
}

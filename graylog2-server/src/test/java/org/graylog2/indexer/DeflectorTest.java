/*
 * Copyright 2012-2014 TORCH GmbH
 *
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
import org.graylog2.indexer.indices.jobs.OptimizeIndexJob;
import org.graylog2.indexer.ranges.RebuildIndexRangesJob;
import org.graylog2.system.activities.ActivityWriter;
import org.graylog2.system.jobs.SystemJobManager;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;
import static org.testng.AssertJUnit.assertEquals;

/**
 *
 * @author lennart.koopmann
 */
public class DeflectorTest {

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
    
    @Test(expectedExceptions=NumberFormatException.class)
    public void testExtractIndexNumberWithMalformedFormatThrowsException() {
        assertEquals(0, Deflector.extractIndexNumber("graylog2_hunderttausend"));
    }
    
    @Test
    public void testBuildIndexName() {
        Deflector d = new Deflector(mock(SystemJobManager.class),
                mock(Configuration.class),
                mock(ActivityWriter.class),
                mock(RebuildIndexRangesJob.Factory.class),
                mock(OptimizeIndexJob.Factory.class));

        assertEquals("graylog2_0", d.buildIndexName("graylog2", 0));
        assertEquals("graylog2_1", d.buildIndexName("graylog2", 1));
        assertEquals("graylog2_9001", d.buildIndexName("graylog2", 9001));
    }

    @Test
    public void testBuildDeflectorNameWithCustomIndexPrefix() {
        assertEquals("foo_custom_index_deflector", Deflector.buildName("foo_custom_index"));
    }

    @Test
    public void nullIndexerDoesNotThrow() {
        Deflector d = new Deflector(mock(SystemJobManager.class),
                                    mock(Configuration.class),
                                    mock(ActivityWriter.class),
                                    mock(RebuildIndexRangesJob.Factory.class),
                                    mock(OptimizeIndexJob.Factory.class));
        final Indexer indexer = mock(Indexer.class);
        when(indexer.indices()).thenReturn(null);
        try {
            final Map<String, IndexStats> deflectorIndices = d.getAllDeflectorIndices(indexer);
            assertNotNull(deflectorIndices);
            Assert.assertEquals(deflectorIndices.size(), 0);
        } catch (Exception e) {
            fail("Should not throw an exception", e);
        }
    }

    @Test
    public void nullIndexerDoesNotThrowOnIndexName() {
        Deflector d = new Deflector(mock(SystemJobManager.class),
                                    mock(Configuration.class),
                                    mock(ActivityWriter.class),
                                    mock(RebuildIndexRangesJob.Factory.class),
                                    mock(OptimizeIndexJob.Factory.class));
        final Indexer indexer = mock(Indexer.class);
        when(indexer.indices()).thenReturn(null);
        try {
            final String[] deflectorIndices = d.getAllDeflectorIndexNames(indexer);
            assertNotNull(deflectorIndices);
            Assert.assertEquals(deflectorIndices.length, 0);
        } catch (Exception e) {
            fail("Should not throw an exception", e);
        }
    }


}

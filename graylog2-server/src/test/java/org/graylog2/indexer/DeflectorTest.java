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

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.indices.jobs.SetIndexReadOnlyAndCalculateRangeJob;
import org.graylog2.indexer.ranges.CreateNewSingleIndexRangeJob;
import org.graylog2.indexer.ranges.IndexRangeService;
import org.graylog2.system.activities.SystemMessageActivityWriter;
import org.graylog2.system.jobs.SystemJobManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DeflectorTest {
    @Mock
    private SystemJobManager systemJobManager;
    @Mock
    private SystemMessageActivityWriter activityWriter;
    @Mock
    private SetIndexReadOnlyJob.Factory indexReadOnlyJobFactory;
    @Mock
    private CreateNewSingleIndexRangeJob.Factory singleIndexRangeJobFactory;
    @Mock
    private SetIndexReadOnlyAndCalculateRangeJob.Factory setIndexReadOnlyAndCalculateRangeJobFactory;
    @Mock
    private Indices indices;
    private Deflector deflector;

    @Mock
    private IndexRangeService indexRangeService;

    @Before
    public void setUp() {
        deflector = new Deflector(systemJobManager,
            "graylog",
            activityWriter,
            indices,
            indexRangeService,
            setIndexReadOnlyAndCalculateRangeJobFactory);
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
        final Map<String, Set<String>> deflectorIndices = deflector.getAllGraylogDeflectorIndices();
        assertNotNull(deflectorIndices);
        assertEquals(0, deflectorIndices.size());
    }

    @Test
    public void nullIndexerDoesNotThrowOnIndexName() {
        final String[] deflectorIndices = deflector.getAllGraylogIndexNames();
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
    public void testIsGraylogIndex() {
        assertTrue(deflector.isGraylogDeflectorIndex("graylog_1"));
        assertTrue(deflector.isGraylogIndex("graylog_1"));

        assertTrue(deflector.isGraylogDeflectorIndex("graylog_42"));
        assertTrue(deflector.isGraylogIndex("graylog_42"));

        assertTrue(deflector.isGraylogDeflectorIndex("graylog_100000000"));
        assertTrue(deflector.isGraylogIndex("graylog_100000000"));

        // The restored archive indices should NOT be taken into account when getting the new deflector number.
        assertFalse(deflector.isGraylogDeflectorIndex("graylog_42_restored_archive"));
        assertTrue(deflector.isGraylogIndex("graylog_42_restored_archive"));

        assertFalse(deflector.isGraylogDeflectorIndex("graylog_42_restored_archive123"));
        assertFalse(deflector.isGraylogIndex("graylog_42_restored_archive123"));

        assertFalse(deflector.isGraylogDeflectorIndex("graylog_42_restored_archive_123"));
        assertFalse(deflector.isGraylogIndex("graylog_42_restored_archive_123"));

        assertFalse(deflector.isGraylogDeflectorIndex(null));
        assertFalse(deflector.isGraylogIndex(null));

        assertFalse(deflector.isGraylogDeflectorIndex(""));
        assertFalse(deflector.isGraylogIndex(""));

        assertFalse(deflector.isGraylogDeflectorIndex("graylog_deflector"));
        assertFalse(deflector.isGraylogIndex("graylog_deflector"));

        assertFalse(deflector.isGraylogDeflectorIndex("graylog2beta_1"));
        assertFalse(deflector.isGraylogIndex("graylog2beta_1"));

        assertFalse(deflector.isGraylogDeflectorIndex("graylog_1_suffix"));
        assertFalse(deflector.isGraylogIndex("graylog_1_suffix"));

        assertFalse(deflector.isGraylogDeflectorIndex("HAHA"));
        assertFalse(deflector.isGraylogIndex("HAHA"));
    }

    @Test
    public void getNewestTargetNumber() throws NoTargetIndexException {
        final Indices indices = mock(Indices.class);
        Map<String, Set<String>> indexNameAliases = Maps.newHashMap();
        indexNameAliases.put("graylog_1", Collections.emptySet());
        indexNameAliases.put("graylog_2", Collections.emptySet());
        indexNameAliases.put("graylog_3", Collections.singleton("graylog_deflector"));
        indexNameAliases.put("graylog_4_restored_archive", Collections.emptySet());

        when(indices.getIndexNamesAndAliases(anyString())).thenReturn(indexNameAliases);
        final Deflector deflector = new Deflector(systemJobManager,
            "graylog",
            activityWriter,
            indices,
            indexRangeService,
            setIndexReadOnlyAndCalculateRangeJobFactory);

        final int number = deflector.getNewestTargetNumber();
        assertEquals(3, number);
    }

    @Test
    public void getAllGraylogIndexNames() {
        final Indices indices = mock(Indices.class);
        Map<String, Set<String>> indexNameAliases = Maps.newHashMap();
        indexNameAliases.put("graylog_1", Collections.emptySet());
        indexNameAliases.put("graylog_2", Collections.emptySet());
        indexNameAliases.put("graylog_3", Collections.emptySet());
        indexNameAliases.put("graylog_4_restored_archive", Collections.emptySet());
        indexNameAliases.put("graylog_5", Collections.singleton("graylog_deflector"));

        when(indices.getIndexNamesAndAliases(anyString())).thenReturn(indexNameAliases);
        final Deflector deflector = new Deflector(systemJobManager,
            "graylog",
            activityWriter,
            indices,
            indexRangeService,
            setIndexReadOnlyAndCalculateRangeJobFactory);

        final String[] allGraylogIndexNames = deflector.getAllGraylogIndexNames();
        assertThat(allGraylogIndexNames)
            .containsExactlyInAnyOrder("graylog_1", "graylog_2", "graylog_3", "graylog_4_restored_archive", "graylog_5");
    }

    @Test
    public void getAllGraylogDeflectorIndices() {
        final Indices indices = mock(Indices.class);
        Map<String, Set<String>> indexNameAliases = Maps.newHashMap();
        indexNameAliases.put("graylog_1", Collections.emptySet());
        indexNameAliases.put("graylog_2", Collections.emptySet());
        indexNameAliases.put("graylog_3", Collections.emptySet());
        indexNameAliases.put("graylog_4_restored_archive", Collections.emptySet());
        indexNameAliases.put("graylog_5", Collections.singleton("graylog_deflector"));

        when(indices.getIndexNamesAndAliases(anyString())).thenReturn(indexNameAliases);
        final Deflector deflector = new Deflector(systemJobManager,
            "graylog",
            activityWriter,
            indices,
            indexRangeService,
            setIndexReadOnlyAndCalculateRangeJobFactory);

        final Map<String, Set<String>> deflectorIndices = deflector.getAllGraylogDeflectorIndices();

        assertThat(deflectorIndices).isNotNull();
        assertThat(deflectorIndices).isNotEmpty();
        assertThat(deflectorIndices.keySet())
            .containsExactlyInAnyOrder("graylog_1", "graylog_2", "graylog_3", "graylog_5");
    }

    @Test
    public void testCleanupAliases() throws Exception {
        final Indices indices = mock(Indices.class);
        Map<String, Set<String>> indexNameAliases = Maps.newHashMap();
        indexNameAliases.put("graylog_1", Collections.emptySet());
        indexNameAliases.put("graylog_2", Collections.singleton("graylog_deflector"));
        indexNameAliases.put("graylog_3", Collections.singleton("graylog_deflector"));
        indexNameAliases.put("foobar", Collections.singleton("graylog_deflector"));

        when(indices.getIndexNamesAndAliases(anyString())).thenReturn(indexNameAliases);
        final Deflector deflector = new Deflector(systemJobManager,
                "graylog",
                activityWriter,
                indices,
                indexRangeService,
                setIndexReadOnlyAndCalculateRangeJobFactory);

        deflector.cleanupAliases(Sets.newHashSet("graylog_2", "graylog_3", "foobar"));

        verify(indices).removeAliases("graylog_deflector", Sets.newHashSet("graylog_2", "foobar"));
    }
}

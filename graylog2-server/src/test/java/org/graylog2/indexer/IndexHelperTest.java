/**
 * Copyright 2012 Lennart Koopmann <lennart@socketfeed.com>
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
 *
 */
package org.graylog2.indexer;

import com.google.common.collect.Sets;
import java.util.Set;
import org.testng.annotations.Test;
import static org.testng.AssertJUnit.*;

/**
 * @author lennart.koopmann
 */
public class IndexHelperTest {

    @Test
    public void testGetOldestIndices() {
        Set<String> indices = Sets.newHashSet();
        indices.add("graylog2_production_1");
        indices.add("graylog2_production_7");
        indices.add("graylog2_production_0");
        indices.add("graylog2_production_2");
        indices.add("graylog2_production_4");
        indices.add("graylog2_production_6");
        indices.add("graylog2_production_3");
        indices.add("graylog2_production_5");
        indices.add("graylog2_production_8");
        indices.add("graylog2_production_9");
        indices.add("graylog2_production_10");
        indices.add("graylog2_production_110");
        indices.add("graylog2_production_125");
        indices.add("graylog2_production_20");
        indices.add("graylog2_production_21");

        Set<String> expected1 = Sets.newHashSet();
        expected1.add("graylog2_production_0");
        expected1.add("graylog2_production_1");
        expected1.add("graylog2_production_2");
        expected1.add("graylog2_production_3");
        expected1.add("graylog2_production_4");
        expected1.add("graylog2_production_5");
        expected1.add("graylog2_production_6");
        
        Set<String> expected2 = Sets.newHashSet();
        expected2.add("graylog2_production_0");

        assertEquals(expected1, IndexHelper.getOldestIndices(indices, 7));
        assertEquals(expected2, IndexHelper.getOldestIndices(indices, 1));
    }
    
    @Test
    public void testGetOldestIndicesWithEmptySetAndTooHighOffset() {
        Set<String> empty = Sets.newHashSet();
        Set<String> expected = Sets.newHashSet();

        assertEquals(expected, IndexHelper.getOldestIndices(empty, 9001));
    }
    
}

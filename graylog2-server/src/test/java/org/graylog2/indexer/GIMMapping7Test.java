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
package org.graylog2.indexer;

import org.graylog2.indexer.indexset.IndexSetConfig;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

class GIMMapping7Test extends GIMMappingTest {
    @Test
    void matchesJsonSource() throws Exception {
        final IndexMappingTemplate template = new GIMMapping7();
        final IndexSetConfig indexSetConfig = mockIndexSetConfig();

        final Map<String, Object> result = template.toTemplate(indexSetConfig, "myindex*", -2147483648);

        assertEquals(resource("expected_gim_template7.json"), json(result), true);
    }
}

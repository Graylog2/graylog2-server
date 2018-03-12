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
package org.graylog2.indexer.fieldtypes;

import org.junit.Before;
import org.junit.Test;

import static com.google.common.collect.ImmutableSet.copyOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.indexer.fieldtypes.FieldTypes.Type.createType;

public class FieldTypeMapperTest {
    private FieldTypeMapper mapper;

    @Before
    public void setUp() throws Exception {
        this.mapper = new FieldTypeMapper();
    }

    private void assertMapping(String esType, String glType, String... properties) {
        assertThat(mapper.mapType(esType))
                .isPresent().get()
                .isEqualTo(createType(glType, copyOf(properties)));
    }

    @Test
    public void mappings() {
        assertMapping("text", "string", "full-text-search");
        assertMapping("keyword", "string", "full-text-search");
        assertMapping("string", "string", "full-text-search");

        assertMapping("long", "long", "numeric", "enumerable");
        assertMapping("integer", "int", "numeric", "enumerable");
        assertMapping("short", "short", "numeric", "enumerable");
        assertMapping("byte", "byte", "numeric", "enumerable");
        assertMapping("double", "double", "numeric", "enumerable");
        assertMapping("float", "float", "numeric", "enumerable");
        assertMapping("half_float", "float", "numeric", "enumerable");
        assertMapping("scaled_float", "float", "numeric", "enumerable");

        assertMapping("date", "date", "enumerable");
        assertMapping("boolean", "boolean", "enumerable");
        assertMapping("binary", "binary");
        assertMapping("geo_point", "geo-point");
        assertMapping("ip", "ip", "enumerable");
    }
}
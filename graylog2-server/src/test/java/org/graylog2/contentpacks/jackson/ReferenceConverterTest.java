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
package org.graylog2.contentpacks.jackson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.graylog2.contentpacks.model.entities.references.Reference;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ReferenceConverterTest {
    private ReferenceConverter converter;
    private ObjectMapper om;

    @Before
    public void setUp() throws Exception {
        converter = new ReferenceConverter();
        om = new ObjectMapperProvider().get();
    }

    private Reference createReference(String type, Object value) {
        return converter.convert(om.convertValue(ImmutableMap.of("@type", type, "@value", value), JsonNode.class));
    }

    @Test
    public void convertBooleanValue() {
        final Reference reference = createReference("boolean", false);

        assertThat(reference).isEqualTo(ValueReference.of(false));
    }

    @Test
    public void convertDoubleValue() {
        final Reference reference = createReference("double", 10d);

        assertThat(reference).isEqualTo(ValueReference.of(10d));
    }

    @Test
    public void convertFloatValue() {
        final Reference reference = createReference("float", 100f);

        assertThat(reference).isEqualTo(ValueReference.of(100f));
    }

    @Test
    public void convertIntegerValue() {
        final Reference reference = createReference("integer", 0);

        assertThat(reference).isEqualTo(ValueReference.of(0));
    }

    @Test
    public void convertLongValue() {
        final Reference reference = createReference("long", 1);

        assertThat(reference).isEqualTo(ValueReference.of(1L));
    }

    @Test
    public void convertStringValue() {
        final Reference reference = createReference("string", "yolo");

        assertThat(reference).isEqualTo(ValueReference.of("yolo"));
    }

    @Test
    public void convertParameterValue() {
        final Reference reference = createReference("parameter", "wat");

        assertThat(reference).isEqualTo(ValueReference.createParameter("wat"));
    }
}
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
package org.graylog2.contentpacks.model.entities.references;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class ReferenceMapUtilsTest {
    @Test
    public void valueReferenceMapOf() {
        final ImmutableMap<String, Object> map = ImmutableMap.<String, Object>builder()
                .put("boolean", true)
                .put("float", 1.0f)
                .put("integer", 42)
                .put("string", "String")
                .put("enum", TestEnum.A)
                .put("list", ImmutableList.of(1, 2.0f, "3", true))
                .put("map", ImmutableMap.of(
                        "k1", "v1",
                        "k2", 2))
                .build();
        final ReferenceMap expectedMap = new ReferenceMap(ImmutableMap.<String, Reference>builder()
                .put("boolean", ValueReference.of(true))
                .put("float", ValueReference.of(1.0f))
                .put("integer", ValueReference.of(42))
                .put("string", ValueReference.of("String"))
                .put("enum", ValueReference.of(TestEnum.A))
                .put("list", new ReferenceList(ImmutableList.of(
                        ValueReference.of(1),
                        ValueReference.of(2.0f),
                        ValueReference.of("3"),
                        ValueReference.of(true))))
                .put("map", new ReferenceMap(ImmutableMap.of(
                        "k1", ValueReference.of("v1"),
                        "k2", ValueReference.of(2))))
                .build());

        final ReferenceMap valueReferenceMap = ReferenceMapUtils.toReferenceMap(map);
        assertThat(valueReferenceMap).isEqualTo(expectedMap);
    }

    @Test
    public void valueMapOf() {
        final ReferenceMap map = new ReferenceMap(ImmutableMap.<String, Reference>builder()
                .put("boolean", ValueReference.of(true))
                .put("float", ValueReference.of(1.0f))
                .put("integer", ValueReference.of(42))
                .put("string", ValueReference.of("String"))
                .put("enum", ValueReference.of(TestEnum.A))
                .put("list", new ReferenceList(ImmutableList.of(
                        ValueReference.of(1),
                        ValueReference.of(2.0f),
                        ValueReference.of("3"),
                        ValueReference.of(true))))
                .put("map", new ReferenceMap(ImmutableMap.of(
                        "k1", ValueReference.of("v1"),
                        "k2", ValueReference.of(2))))
                .build());
        final ImmutableMap<String, Object> expectedMap = ImmutableMap.<String, Object>builder()
                .put("boolean", true)
                .put("float", 1.0f)
                .put("integer", 42)
                .put("string", "String")
                .put("enum", "A")
                .put("list", ImmutableList.of(1, 2.0f, "3", true))
                .put("map", ImmutableMap.of(
                        "k1", "v1",
                        "k2", 2))
                .build();

        final Map<String, Object> valueReferenceMap = ReferenceMapUtils.toValueMap(map, Collections.emptyMap());
        assertThat(valueReferenceMap).isEqualTo(expectedMap);
    }

    public enum TestEnum {A, B}
}
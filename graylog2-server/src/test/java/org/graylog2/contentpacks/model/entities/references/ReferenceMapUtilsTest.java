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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ReferenceMapUtilsTest {
    @Test
    public void toReferenceMap() {
        final ImmutableMap<String, Object> map = ImmutableMap.<String, Object>builder()
                .put("boolean", true)
                .put("double", 2.0D)
                .put("float", 1.0f)
                .put("integer", 42)
                .put("long", 10000000000L)
                .put("string", "String")
                .put("enum", TestEnum.A)
                .put("list", ImmutableList.of(1, 2.0f, "3", true))
                .put("map", ImmutableMap.of(
                        "k1", "v1",
                        "k2", 2))
                .build();
        final ReferenceMap expectedMap = new ReferenceMap(ImmutableMap.<String, Reference>builder()
                .put("boolean", ValueReference.of(true))
                .put("double", ValueReference.of(2.0D))
                .put("float", ValueReference.of(1.0f))
                .put("integer", ValueReference.of(42))
                .put("long", ValueReference.of(10000000000L))
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
    public void toValueMap() {
        final Map<String, ValueReference> parameters = ImmutableMap.<String, ValueReference>builder()
                .put("BOOLEAN", ValueReference.of(true))
                .put("FLOAT", ValueReference.of(1.0f))
                .put("INTEGER", ValueReference.of(42))
                .put("STRING", ValueReference.of("String"))
                .put("ENUM", ValueReference.of(TestEnum.A))
                .build();
        final ReferenceMap map = new ReferenceMap(ImmutableMap.<String, Reference>builder()
                .put("boolean", ValueReference.of(true))
                .put("param_boolean", ValueReference.createParameter("BOOLEAN"))
                .put("float", ValueReference.of(1.0f))
                .put("param_float", ValueReference.createParameter("FLOAT"))
                .put("integer", ValueReference.of(42))
                .put("param_integer", ValueReference.createParameter("INTEGER"))
                .put("string", ValueReference.of("String"))
                .put("param_string", ValueReference.createParameter("STRING"))
                .put("enum", ValueReference.of(TestEnum.A))
                .put("param_enum", ValueReference.createParameter("ENUM"))
                .put("list", new ReferenceList(ImmutableList.of(
                        ValueReference.of(1),
                        ValueReference.of(2.0f),
                        ValueReference.of("3"),
                        ValueReference.of(true),
                        ValueReference.createParameter("STRING"))))
                .put("map", new ReferenceMap(ImmutableMap.of(
                        "k1", ValueReference.of("v1"),
                        "k2", ValueReference.of(2),
                        "k3", ValueReference.createParameter("INTEGER"))))
                .build());
        final ImmutableMap<String, Object> expectedMap = ImmutableMap.<String, Object>builder()
                .put("boolean", true)
                .put("param_boolean", true)
                .put("float", 1.0f)
                .put("param_float", 1.0f)
                .put("integer", 42)
                .put("param_integer", 42)
                .put("string", "String")
                .put("param_string", "String")
                .put("enum", "A")
                .put("param_enum", "A")
                .put("list", ImmutableList.of(1, 2.0f, "3", true, "String"))
                .put("map", ImmutableMap.of(
                        "k1", "v1",
                        "k2", 2,
                        "k3", 42))
                .build();

        final Map<String, Object> valueReferenceMap = ReferenceMapUtils.toValueMap(map, parameters);
        assertThat(valueReferenceMap).isEqualTo(expectedMap);
    }

    @Test
    public void toValueMapWithMissingParameter() {
        final Map<String, ValueReference> parameters = Collections.emptyMap();
        final ReferenceMap map = new ReferenceMap(Collections.singletonMap("param", ValueReference.createParameter("STRING")));

        assertThatThrownBy(() -> ReferenceMapUtils.toValueMap(map, parameters))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Missing parameter STRING");
    }

    @Test
    public void toValueMapWithCircularParameter() {
        final Map<String, ValueReference> parameters = Collections.singletonMap("STRING", ValueReference.createParameter("OTHER"));
        final ReferenceMap map = new ReferenceMap(Collections.singletonMap("param", ValueReference.createParameter("STRING")));

        assertThatThrownBy(() -> ReferenceMapUtils.toValueMap(map, parameters))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Circular parameter STRING");
    }

    public enum TestEnum {A, B}
}
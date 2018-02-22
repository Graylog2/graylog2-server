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
package org.graylog.plugins.beats;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class MapUtilsTest {
    @Test
    public void flattenHandlesEmptyMap() throws Exception {
        assertThat(MapUtils.flatten(Collections.emptyMap(), "", "_")).isEmpty();
    }

    @Test
    public void flattenHandlesFlatMap() throws Exception {
        final Map<String, Object> map = ImmutableMap.of(
                "foo", "bar",
                "baz", "qux");
        assertThat(MapUtils.flatten(map, "", "_")).isEqualTo(map);
    }

    @Test
    public void flattenAddsParentKey() throws Exception {
        final Map<String, Object> map = ImmutableMap.of(
                "map", ImmutableMap.of(
                        "foo", "bar",
                        "baz", "qux"));
        final Map<String, Object> expected = ImmutableMap.of(
                "map_foo", "bar",
                "map_baz", "qux");
        assertThat(MapUtils.flatten(map, "", "_")).isEqualTo(expected);
    }

    @Test
    public void flattenAddsParentKeys() throws Exception {
        final Map<String, Object> map = ImmutableMap.of(
                "map", ImmutableMap.of(
                        "foo", "bar",
                        "baz", "qux"));
        final Map<String, Object> expected = ImmutableMap.of(
                "test_map_foo", "bar",
                "test_map_baz", "qux");
        assertThat(MapUtils.flatten(map, "test", "_")).isEqualTo(expected);
    }

    @Test
    public void flattenSupportsMultipleLevels() throws Exception {
        final Map<String, Object> map = ImmutableMap.of(
                "map", ImmutableMap.of(
                        "foo", "bar",
                        "baz", ImmutableMap.of(
                                "foo", "bar",
                                "baz", "qux")));
        final Map<String, Object> expected = ImmutableMap.of(
                "map_foo", "bar",
                "map_baz_foo", "bar",
                "map_baz_baz", "qux");
        assertThat(MapUtils.flatten(map, "", "_")).isEqualTo(expected);
    }

    @Test
    public void renameKeyHandlesEmptyMap() {
        final Map<String, Object> map = new HashMap<>();
        MapUtils.renameKey(map, "foo", "bar");
        assertThat(map).isEmpty();
    }

    @Test
    public void renameKeyMutatesMap() {
        final Map<String, Object> map = new HashMap<>();
        map.put("foo", "test");
        MapUtils.renameKey(map, "foo", "bar");
        assertThat(map).containsEntry("bar", "test");
    }

    @Test
    public void replaceKeyCharacterHandlesEmptyMap() {
        assertThat(MapUtils.replaceKeyCharacter(Collections.emptyMap(), '.', '_')).isEmpty();
    }

    @Test
    public void renameKeyReturnsMutatedMap() {
        final Map<String, Object> map = ImmutableMap.of(
                "foo.bar", "test",
                "baz@quux", "test");
        assertThat(MapUtils.replaceKeyCharacter(map, '.', '_'))
                .hasSameSizeAs(map)
                .containsEntry("foo_bar", "test")
                .containsEntry("baz@quux", "test");
    }
}
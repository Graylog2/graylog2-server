/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
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
package org.graylog2.inputs.misc.jsonpath;

import com.google.common.collect.Maps;
import com.jayway.jsonpath.JsonPath;
import org.testng.annotations.Test;

import java.util.Map;

import static org.testng.AssertJUnit.assertEquals;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */

public class SelectorTest {

    private static final double DELTA = 1e-15;

    @Test
    public void testReadResultingInSingleInteger() throws Exception {
        String json = "{\"url\":\"https://api.github.com/repos/Graylog2/graylog2-server/releases/assets/22660\",\"download_count\":76185,\"id\":22660,\"name\":\"graylog2-server-0.20.0-preview.1.tgz\",\"label\":\"graylog2-server-0.20.0-preview.1.tgz\",\"content_type\":\"application/octet-stream\",\"state\":\"uploaded\",\"size\":38179285,\"updated_at\":\"2013-09-30T20:05:46Z\"}";
        JsonPath path = JsonPath.compile("$.download_count");

        Map<String, Object> result = new Selector(path).read(json);
        assertEquals(1, result.size());
        assertEquals(76185, result.get("result"));
    }

    @Test
    public void testReadResultingInSingleString() throws Exception {
        String json = "{\"url\":\"https://api.github.com/repos/Graylog2/graylog2-server/releases/assets/22660\",\"download_count\":76185,\"id\":22660,\"name\":\"graylog2-server-0.20.0-preview.1.tgz\",\"label\":\"graylog2-server-0.20.0-preview.1.tgz\",\"content_type\":\"application/octet-stream\",\"state\":\"uploaded\",\"size\":38179285,\"updated_at\":\"2013-09-30T20:05:46Z\"}";
        JsonPath path = JsonPath.compile("$.state");

        Map<String, Object> result = new Selector(path).read(json);
        assertEquals(1, result.size());
        assertEquals("uploaded", result.get("result"));
    }

    @Test
    public void testReadFromMap() throws Exception {
        String json = "{\"store\":{\"book\":[{\"category\":\"reference\",\"author\":\"Nigel Rees\",\"title\":\"Sayings of the Century\",\"price\":8.95},{\"category\":\"fiction\",\"author\":\"Evelyn Waugh\",\"title\":\"Sword of Honour\",\"price\":12.99,\"isbn\":\"0-553-21311-3\"}],\"bicycle\":{\"color\":\"red\",\"price\":19.95}}}";
        JsonPath path = JsonPath.compile("$.store.book[?(@.category == 'fiction')].author");

        Map<String, Object> result = new Selector(path).read(json);
        assertEquals(1, result.size());
        assertEquals("Evelyn Waugh", result.get("result"));
    }

    @Test
    public void testReadResultingInDouble() throws Exception {
        String json = "{\"url\":\"https://api.github.com/repos/Graylog2/graylog2-server/releases/assets/22660\",\"some_double\":0.50,\"id\":22660,\"name\":\"graylog2-server-0.20.0-preview.1.tgz\",\"label\":\"graylog2-server-0.20.0-preview.1.tgz\",\"content_type\":\"application/octet-stream\",\"state\":\"uploaded\",\"size\":38179285,\"updated_at\":\"2013-09-30T20:05:46Z\"}";
        JsonPath path = JsonPath.compile("$.some_double");

        Map<String, Object> result = new Selector(path).read(json);
        assertEquals(1, result.size());
        assertEquals(0.5, (Double) result.get("result"), DELTA);
    }

    @Test
    public void testBuildShortMessage() throws Exception {
        Map<String, Object> fields = Maps.newHashMap();
        fields.put("foo", "bar");
        fields.put("baz", 9001);

        Selector selector = new Selector(JsonPath.compile("$.download_count"));
        assertEquals("JSON API poll result: $.download_count -> {baz=9001, foo=bar}", selector.buildShortMessage(fields));
    }

    @Test
    public void testBuildShortMessageThatGetsCut() throws Exception {
        Map<String, Object> fields = Maps.newHashMap();
        fields.put("foo", "bargggdzrtdfgfdgldfsjgkfdlgjdflkjglfdjgljslfperitperoujglkdnfkndsbafdofhasdpfoöadjsFOO");
        fields.put("baz", 9001);

        Selector selector = new Selector(JsonPath.compile("$.download_count"));
        assertEquals("JSON API poll result: $.download_count -> {baz=9001, foo=bargggdzrtdfgfdgldfsjgkfdlgjdflkjgl[...]", selector.buildShortMessage(fields));
    }

}

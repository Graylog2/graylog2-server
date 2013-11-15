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

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import static junit.framework.Assert.assertEquals;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class JsonPathInputTest {

    @Test
    public void testParseHeaders() throws Exception {
        assertEquals(0, JsonPathInput.parseHeaders("").size());
        assertEquals(0, JsonPathInput.parseHeaders(" ").size());
        assertEquals(0, JsonPathInput.parseHeaders(" . ").size());
        assertEquals(0, JsonPathInput.parseHeaders("foo").size());
        assertEquals(1, JsonPathInput.parseHeaders("X-Foo: Bar").size());

        Map<String, String> expectedSingle = new HashMap<String, String>() {{
            put("Accept", "application/json");
        }};

        Map<String, String> expectedMulti = new HashMap<String, String>() {{
            put("Accept", "application/json");
            put("X-Foo", "bar");
        }};

        assertEquals(expectedMulti, JsonPathInput.parseHeaders("Accept: application/json, X-Foo: bar"));
        assertEquals(expectedSingle, JsonPathInput.parseHeaders("Accept: application/json"));

        assertEquals(expectedMulti, JsonPathInput.parseHeaders(" Accept:  application/json,X-Foo:bar"));
        assertEquals(expectedMulti, JsonPathInput.parseHeaders("Accept:application/json,   X-Foo: bar "));
        assertEquals(expectedMulti, JsonPathInput.parseHeaders("Accept:    application/json,     X-Foo: bar"));
        assertEquals(expectedMulti, JsonPathInput.parseHeaders("Accept :application/json,   X-Foo: bar "));

        assertEquals(expectedSingle, JsonPathInput.parseHeaders(" Accept: application/json"));
        assertEquals(expectedSingle, JsonPathInput.parseHeaders("Accept:application/json"));
        assertEquals(expectedSingle, JsonPathInput.parseHeaders(" Accept: application/json "));
        assertEquals(expectedSingle, JsonPathInput.parseHeaders(" Accept :application/json "));

    }

}

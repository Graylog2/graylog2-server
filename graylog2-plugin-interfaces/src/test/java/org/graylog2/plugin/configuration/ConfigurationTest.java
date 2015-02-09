/**
 * The MIT License
 * Copyright (c) 2012 Graylog, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.graylog2.plugin.configuration;

import com.google.common.collect.ImmutableMap;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.*;

public class ConfigurationTest {

    @Test
    public void testConfigSerialization() throws Exception {
        final ImmutableMap<String, Object> emptyMap = ImmutableMap.of();
        final Configuration emptyConfig = new Configuration(emptyMap);

        assertNull(emptyConfig.serializeToJson());
        assertNull(new Configuration(null).serializeToJson());

        final Map<String, Object> map = new HashMap<>();

        // Test might be broken depending on the iteration order...
        map.put("b", 1);
        map.put("a", 1);

        final String json = new Configuration(map).serializeToJson();
        assertEquals(json, "{\"source\":{\"a\":1,\"b\":1}}");

        final Configuration config = Configuration.deserializeFromJson(json);

        assertTrue(config.intIsSet("a"));
        assertTrue(config.intIsSet("b"));

        final Configuration emptyConfigFromNull = Configuration.deserializeFromJson(null);

        assertNotNull(emptyConfigFromNull);
    }
}
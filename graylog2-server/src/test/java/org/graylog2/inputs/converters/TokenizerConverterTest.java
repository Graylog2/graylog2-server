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
package org.graylog2.inputs.converters;

import org.graylog2.plugin.inputs.Converter;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.assertNull;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class TokenizerConverterTest {

    @Test
    public void testConvert() throws Exception {
        Converter hc = new TokenizerConverter(new HashMap<String, Object>());

        assertNull(hc.convert(null));
        assertEquals("", hc.convert(""));
    }

    @Test
    public void testBasic() {
        TokenizerConverter f = new TokenizerConverter(new HashMap<String, Object>());
        Map<String, String> result = (Map<String, String>) f.convert("Ohai I am a message k1=v1 k2=v2 Awesome!");

        assertEquals(2, result.size());
        assertEquals("v1", result.get("k1"));
        assertEquals("v2", result.get("k2"));
    }


    @Test
    public void testFilterWithKVAtBeginning() {
        TokenizerConverter f = new TokenizerConverter(new HashMap<String, Object>());
        Map<String, String> result = (Map<String, String>) f.convert("k1=v1 k2=v2 Awesome!");

        assertEquals(2, result.size());
        assertEquals("v1", result.get("k1"));
        assertEquals("v2", result.get("k2"));
    }

    @Test
    public void testFilterWithKVAtEnd() {
        TokenizerConverter f = new TokenizerConverter(new HashMap<String, Object>());
        Map<String, String> result = (Map<String, String>) f.convert("lolwat Awesome! k1=v1");

        assertEquals(1, result.size());
        assertEquals("v1", result.get("k1"));
    }

    @Test
    public void testFilterWithStringInBetween() {
        TokenizerConverter f = new TokenizerConverter(new HashMap<String, Object>());
        Map<String, String> result = (Map<String, String>) f.convert("foo k2=v2 lolwat Awesome! k1=v1");

        assertEquals(2, result.size());
        assertEquals("v1", result.get("k1"));
        assertEquals("v2", result.get("k2"));
    }

    @Test
    public void testFilterWithKVOnly() {
        TokenizerConverter f = new TokenizerConverter(new HashMap<String, Object>());
        Map<String, String> result = (Map<String, String>) f.convert("k1=v1");

        assertEquals(1, result.size());
        assertEquals("v1", result.get("k1"));
    }

    @Test
    public void testFilterWithInvalidKVPairs() {
        TokenizerConverter f = new TokenizerConverter(new HashMap<String, Object>());
        Map<String, String> result = (Map<String, String>) f.convert("Ohai I am a message and this is a URL: index.php?foo=bar&baz=bar");

        assertEquals(0, result.size());
    }

    @Test
    public void testFilterWithoutKVPairs() {
        TokenizerConverter f = new TokenizerConverter(new HashMap<String, Object>());
        Map<String, String> result = (Map<String, String>) f.convert("trolololololol");


        assertEquals(0, result.size());
    }

    @Test
    public void testFilterWithOneInvalidKVPair() {
        TokenizerConverter f = new TokenizerConverter(new HashMap<String, Object>());
        Map<String, String> result = (Map<String, String>) f.convert("Ohai I am a message and this is a URL: index.php?foo=bar");

        assertEquals(0, result.size());
    }

    @Test
    public void testFilterWithWhitespaceAroundKVNoException() {
        TokenizerConverter f = new TokenizerConverter(new HashMap<String, Object>());
        Map<String, String> result = (Map<String, String>) f.convert("k1 = ");

        assertEquals(0, result.size());
    }

    @Test
    public void testFilterWithWhitespaceAroundKV() {
        TokenizerConverter f = new TokenizerConverter(new HashMap<String, Object>());
        Map<String, String> result = (Map<String, String>) f.convert("otters in k1 = v1 k2= v2 k3 =v3 k4=v4 more otters");

        assertEquals(4, result.size());
        assertEquals("v1", result.get("k1"));
        assertEquals("v2", result.get("k2"));
        assertEquals("v3", result.get("k3"));
        assertEquals("v4", result.get("k4"));
    }

    @Test
    public void testFilterWithQuotedValue() {
        TokenizerConverter f = new TokenizerConverter(new HashMap<String, Object>());
        Map<String, String> result = (Map<String, String>) f.convert("otters in k1=\"v1\" more otters");

        assertEquals(1, result.size());
        assertEquals("v1", result.get("k1"));
    }

    @Test
    public void testFilterWithIDAdditionalField() {
        TokenizerConverter f = new TokenizerConverter(new HashMap<String, Object>());
        Map<String, String> result = (Map<String, String>) f.convert("otters _id=123 more otters");

        assertTrue(result.get("_id") != "123");
    }

}

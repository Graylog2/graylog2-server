/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.inputs.converters;

import org.graylog2.plugin.inputs.Converter;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

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
        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) f.convert("Ohai I am a message k1=v1 k2=v2 Awesome!");

        assertEquals(2, result.size());
        assertEquals("v1", result.get("k1"));
        assertEquals("v2", result.get("k2"));
    }


    @Test
    public void testFilterWithKVAtBeginning() {
        TokenizerConverter f = new TokenizerConverter(new HashMap<String, Object>());
        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) f.convert("k1=v1 k2=v2 Awesome!");

        assertEquals(2, result.size());
        assertEquals("v1", result.get("k1"));
        assertEquals("v2", result.get("k2"));
    }

    @Test
    public void testFilterWithKVAtEnd() {
        TokenizerConverter f = new TokenizerConverter(new HashMap<String, Object>());
        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) f.convert("lolwat Awesome! k1=v1");

        assertEquals(1, result.size());
        assertEquals("v1", result.get("k1"));
    }

    @Test
    public void testFilterWithStringInBetween() {
        TokenizerConverter f = new TokenizerConverter(new HashMap<String, Object>());
        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) f.convert("foo k2=v2 lolwat Awesome! k1=v1");

        assertEquals(2, result.size());
        assertEquals("v1", result.get("k1"));
        assertEquals("v2", result.get("k2"));
    }

    @Test
    public void testFilterWithKVOnly() {
        TokenizerConverter f = new TokenizerConverter(new HashMap<String, Object>());
        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) f.convert("k1=v1");

        assertEquals(1, result.size());
        assertEquals("v1", result.get("k1"));
    }

    @Test
    public void testFilterWithInvalidKVPairs() {
        TokenizerConverter f = new TokenizerConverter(new HashMap<String, Object>());
        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) f.convert("Ohai I am a message and this is a URL: index.php?foo=bar&baz=bar");

        assertEquals(0, result.size());
    }

    @Test
    public void testFilterWithoutKVPairs() {
        TokenizerConverter f = new TokenizerConverter(new HashMap<String, Object>());
        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) f.convert("trolololololol");


        assertEquals(0, result.size());
    }

    @Test
    public void testFilterWithOneInvalidKVPair() {
        TokenizerConverter f = new TokenizerConverter(new HashMap<String, Object>());
        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) f.convert("Ohai I am a message and this is a URL: index.php?foo=bar");

        assertEquals(0, result.size());
    }

    @Test
    public void testFilterWithWhitespaceAroundKVNoException() {
        TokenizerConverter f = new TokenizerConverter(new HashMap<String, Object>());
        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) f.convert("k1 = ");

        assertEquals(0, result.size());
    }

    @Test
    public void testFilterWithWhitespaceAroundKV() {
        TokenizerConverter f = new TokenizerConverter(new HashMap<String, Object>());
        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) f.convert("otters in k1 = v1 k2= v2 k3 =v3 k4=v4 more otters");

        assertEquals(4, result.size());
        assertEquals("v1", result.get("k1"));
        assertEquals("v2", result.get("k2"));
        assertEquals("v3", result.get("k3"));
        assertEquals("v4", result.get("k4"));
    }

    @Test
    public void testFilterWithNewlineBetweenKV() {
        final TokenizerConverter f = new TokenizerConverter(new HashMap<>());
        @SuppressWarnings("unchecked")
        final Map<String, String> result = (Map<String, String>) f.convert("otters in k1 = v1\nk2= v2 more otters");

        assertEquals(2, result.size());
        assertEquals("v1", result.get("k1"));
        assertEquals("v2", result.get("k2"));
    }

    @Test
    public void testFilterWithQuotedValue() {
        TokenizerConverter f = new TokenizerConverter(new HashMap<String, Object>());
        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) f.convert("otters in k1=\"v1\" more otters");

        assertEquals(1, result.size());
        assertEquals("v1", result.get("k1"));
    }
    
    @Test
    public void testFilterWithSingleQuotedValue() {
        TokenizerConverter f = new TokenizerConverter(new HashMap<String, Object>());
        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) f.convert("otters in k1='v1' more otters");

        assertEquals(1, result.size());
        assertEquals("v1", result.get("k1"));
    }

    @Test
    public void testFilterWithIDAdditionalField() {
        TokenizerConverter f = new TokenizerConverter(new HashMap<String, Object>());
        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) f.convert("otters _id=123 more otters");

        assertEquals(1, result.size());
        assertEquals("123", result.get("_id"));
    }

    @Test
    public void testFilterWithMixedQuotedAndPlainValues() {
        TokenizerConverter f = new TokenizerConverter(new HashMap<String, Object>());
        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) f.convert("otters in k1=\"v1\" k2=v2 more otters");

        assertThat(result)
                .hasSize(2)
                .containsEntry("k1", "v1")
                .containsEntry("k2", "v2");
    }
    
    @Test
    public void testFilterWithMixedSingleQuotedAndPlainValues() {
        TokenizerConverter f = new TokenizerConverter(new HashMap<String, Object>());
        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) f.convert("otters in k1='v1' k2=v2 more otters");

        assertThat(result)
                .hasSize(2)
                .containsEntry("k1", "v1")
                .containsEntry("k2", "v2");
    }

    @Test
    public void testFilterWithKeysIncludingDashOrUnderscore() {
        TokenizerConverter f = new TokenizerConverter(new HashMap<String, Object>());
        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) f.convert("otters in k-1=v1 k_2=v2 _k3=v3 more otters");

        assertThat(result)
                .hasSize(3)
                .containsEntry("k-1", "v1")
                .containsEntry("k_2", "v2")
                .containsEntry("_k3", "v3");
    }

    @Test
    public void testFilterRetainsWhitespaceInQuotedValues() {
        TokenizerConverter f = new TokenizerConverter(new HashMap<String, Object>());
        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) f.convert("otters in k1= v1  k2=\" v2\" k3=\" v3 \" more otters");

        assertThat(result)
                .hasSize(3)
                .containsEntry("k1", "v1")
                .containsEntry("k2", " v2")
                .containsEntry("k3", " v3 ");
    }
    
    @Test
    public void testFilterRetainsWhitespaceInSingleQuotedValues() {
        TokenizerConverter f = new TokenizerConverter(new HashMap<String, Object>());
        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) f.convert("otters in k1= v1  k2=' v2' k3=' v3 ' more otters");

        assertThat(result)
                .hasSize(3)
                .containsEntry("k1", "v1")
                .containsEntry("k2", " v2")
                .containsEntry("k3", " v3 ");
    }
    
    @Test 
    public void testFilterRetainsNestedSingleQuotesInDoubleQuotedValues() {
        TokenizerConverter f = new TokenizerConverter(new HashMap<String, Object>());
        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) f.convert("otters in k1= v1  k2=\" 'v2'\" k3=\" 'v3' \" more otters");

        assertThat(result)
                .hasSize(3)
                .containsEntry("k1", "v1")
                .containsEntry("k2", " 'v2'")
                .containsEntry("k3", " 'v3' ");
    }
    
    @Test
    public void testFilterRetainsNestedDoubleQuotesInSingleQuotedValues() {
        TokenizerConverter f = new TokenizerConverter(new HashMap<String, Object>());
        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) f.convert("otters in k1= v1  k2=' \"v2\"' k3=' \"v3\" ' more otters");

        assertThat(result)
                .hasSize(3)
                .containsEntry("k1", "v1")
                .containsEntry("k2", " \"v2\"")
                .containsEntry("k3", " \"v3\" ");
    }

    @Test
    public void testFilterSupportsMultipleIdenticalKeys() {
        TokenizerConverter f = new TokenizerConverter(new HashMap<String, Object>());
        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) f.convert("Ohai I am a message k1=v1 k1=v2 Awesome!");

        assertEquals(1, result.size());
        assertEquals("v2", result.get("k1"));
    }
}

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

import org.graylog2.ConfigurationException;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class SplitAndCountConverterTest {
    @Test
    public void testConvert() throws Exception {
        assertEquals(0, new SplitAndCountConverter(config("x")).convert(""));
        assertEquals(1, new SplitAndCountConverter(config("_")).convert("foo-bar-baz"));
        assertEquals(1, new SplitAndCountConverter(config("-")).convert("foo"));
        assertEquals(2, new SplitAndCountConverter(config("-")).convert("foo-bar"));
        assertEquals(3, new SplitAndCountConverter(config("-")).convert("foo-bar-baz"));
        assertEquals(3, new SplitAndCountConverter(config(".")).convert("foo.bar.baz")); // Regex. Must be escaped.
    }

    @Test(expected = ConfigurationException.class)
    public void testWithEmptyConfig() throws Exception {
        assertEquals(null, new SplitAndCountConverter(config("")).convert("foo"));
    }

    @Test(expected = ConfigurationException.class)
    public void testWithNullConfig() throws Exception {
        assertEquals(null, new SplitAndCountConverter(config(null)).convert("foo"));
    }

    private Map<String, Object> config(final String splitBy) {
        return Collections.singletonMap("split_by", splitBy);
    }
}

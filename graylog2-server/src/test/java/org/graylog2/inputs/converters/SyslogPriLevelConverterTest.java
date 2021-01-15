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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class SyslogPriLevelConverterTest {

    @Test
    public void testConvert() throws Exception {
        Converter hc = new SyslogPriLevelConverter(new HashMap<String, Object>());
        assertNull(hc.convert(null));
        assertEquals("", hc.convert(""));
        assertEquals("lol no number", hc.convert("lol no number"));

        assertEquals(6, hc.convert("14")); // info
        assertEquals(4, hc.convert("12")); // warning
        assertEquals(7, hc.convert("7")); // debug
        assertEquals(7, hc.convert("87")); // debug
        assertEquals(5, hc.convert("5")); // notice
    }

}

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
public class NumericConverterTest {

    @Test
    public void testConvert() throws Exception {
        Converter hc = new NumericConverter(new HashMap<String, Object>());

        assertNull(hc.convert(null));
        assertEquals("", hc.convert(""));
        assertEquals("lol no number", hc.convert("lol no number"));
        assertEquals(9001, hc.convert("9001"));
        assertEquals(2147483648L, hc.convert("2147483648"));
        assertEquals(10.4D, hc.convert("10.4"));
        assertEquals(Integer.class, hc.convert("4").getClass());
    }

}

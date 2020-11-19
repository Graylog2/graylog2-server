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
public class IPAnonymizerConverterTest {

    @Test
    public void testConvert() throws Exception {
        Converter hc = new IPAnonymizerConverter(new HashMap<String, Object>());

        assertNull(hc.convert(null));
        assertEquals("", hc.convert(""));
        assertEquals("lol no IP in here", hc.convert("lol no IP in here"));
        assertEquals("127.0.1", hc.convert("127.0.1"));
        assertEquals("127.0.0.xxx", hc.convert("127.0.0.xxx"));

        assertEquals("127.0.0.xxx", hc.convert("127.0.0.1"));
        assertEquals("127.0.0.xxx foobar 192.168.1.xxx test", hc.convert("127.0.0.1 foobar 192.168.1.100 test"));
    }

}

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
package org.graylog2.inputs.transports;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import java.util.Map;

import static org.graylog2.inputs.transports.HttpPollTransport.parseHeaders;
import static org.junit.Assert.assertEquals;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class HttpPollTransportTest {

    @Test
    public void testParseHeaders() throws Exception {
        assertEquals(0, parseHeaders("").size());
        assertEquals(0, parseHeaders(" ").size());
        assertEquals(0, parseHeaders(" . ").size());
        assertEquals(0, parseHeaders("foo").size());
        assertEquals(1, parseHeaders("X-Foo: Bar").size());

        Map<String, String> expectedSingle = ImmutableMap.of("Accept", "application/json");
        Map<String, String> expectedMulti = ImmutableMap.of(
                "Accept", "application/json",
                "X-Foo", "bar");

        assertEquals(expectedMulti, parseHeaders("Accept: application/json, X-Foo: bar"));
        assertEquals(expectedSingle, parseHeaders("Accept: application/json"));

        assertEquals(expectedMulti, parseHeaders(" Accept:  application/json,X-Foo:bar"));
        assertEquals(expectedMulti, parseHeaders("Accept:application/json,   X-Foo: bar "));
        assertEquals(expectedMulti, parseHeaders("Accept:    application/json,     X-Foo: bar"));
        assertEquals(expectedMulti, parseHeaders("Accept :application/json,   X-Foo: bar "));

        assertEquals(expectedSingle, parseHeaders(" Accept: application/json"));
        assertEquals(expectedSingle, parseHeaders("Accept:application/json"));
        assertEquals(expectedSingle, parseHeaders(" Accept: application/json "));
        assertEquals(expectedSingle, parseHeaders(" Accept :application/json "));

    }

}

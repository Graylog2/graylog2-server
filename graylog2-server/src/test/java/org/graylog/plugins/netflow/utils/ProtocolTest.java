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
package org.graylog.plugins.netflow.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ProtocolTest {
    @Test
    public void test() throws Exception {
        final Protocol tcp = Protocol.TCP;

        assertEquals("tcp", tcp.getName());
        assertEquals(6, tcp.getNumber());
        assertEquals("TCP", tcp.getAlias());
    }

    @Test
    public void testGetByNumber() throws Exception {
        assertEquals(Protocol.TCP, Protocol.getByNumber(6));
        assertEquals(Protocol.VRRP, Protocol.getByNumber(112));
    }

    @Test
    public void testNull() throws Exception {
        assertNull(Protocol.getByNumber(1231323424));
    }
}

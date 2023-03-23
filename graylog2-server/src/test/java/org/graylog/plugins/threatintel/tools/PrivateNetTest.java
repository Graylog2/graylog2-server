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
package org.graylog.plugins.threatintel.tools;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class PrivateNetTest {

    @Test
    public void testIsInPrivateAddressSpace() throws Exception {
        assertTrue(PrivateNet.isInPrivateAddressSpace("10.0.0.1"));
        assertTrue(PrivateNet.isInPrivateAddressSpace("172.16.20.50"));
        assertTrue(PrivateNet.isInPrivateAddressSpace("192.168.1.1"));
        assertFalse(PrivateNet.isInPrivateAddressSpace("99.42.44.219"));
        assertFalse(PrivateNet.isInPrivateAddressSpace("ff02:0:0:0:0:0:0:fb"));
        assertTrue(PrivateNet.isInPrivateAddressSpace("fd80:0:0:0:0:0:0:fb"));
        assertThrows(IllegalArgumentException.class, () -> PrivateNet.isInPrivateAddressSpace("this is not an IP address"));
    }

}

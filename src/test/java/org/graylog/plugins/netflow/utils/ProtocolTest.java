/**
 * Copyright (C) 2012, 2013, 2014 wasted.io Ltd <really@wasted.io>
 * Copyright (C) 2015 Graylog, Inc. (hello@graylog.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

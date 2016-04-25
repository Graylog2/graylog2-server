/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.utilities;

import org.jboss.netty.handler.ipfilter.IpSubnet;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class IPSubnetConverterTest {

    IPSubnetConverter converter = new IPSubnetConverter();

    @Test
    public void testDefault() throws Exception {
        final String defaultList = "127.0.0.1/32,0:0:0:0:0:0:0:1/128";
        Set<IpSubnet> results = converter.convertFrom(defaultList);
        assertEquals(results.size(), 2);
        assertEquals(defaultList, converter.convertTo(results));
        IpSubnet subnets[] = new IpSubnet[results.size()];
        results.toArray(subnets);
        assertEquals("127.0.0.1/32", subnets[0].toString());
        assertEquals("0:0:0:0:0:0:0:1/128", subnets[1].toString());
    }

    @Test
    public void testNormalize() throws Exception {
        final String defaultList = "127.0.0.1/32, ::1/128";
        final String normalized = "127.0.0.1/32,0:0:0:0:0:0:0:1/128";
        Set<IpSubnet> results = converter.convertFrom(defaultList);
        assertEquals(normalized, converter.convertTo(results));
    }

    @Test
    public void testNull() throws Exception {
        Set<IpSubnet> results = converter.convertFrom(null);
        assertNotNull(results);
        assertEquals(results.size(), 0);
    }

}

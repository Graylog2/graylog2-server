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

import com.github.joschi.jadconfig.ParameterException;
import org.jboss.netty.handler.ipfilter.IpSubnet;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class IPSubnetConverterTest {
    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    private final IPSubnetConverter converter = new IPSubnetConverter();

    @Test
    public void testDefault() throws Exception {
        final String defaultList = "127.0.0.1/32,0:0:0:0:0:0:0:1/128";
        final Set<IpSubnet> results = converter.convertFrom(defaultList);
        assertThat(results)
            .hasSize(2)
            .contains(new IpSubnet(Inet4Address.getByName("127.0.0.1"), 32))
            .contains(new IpSubnet(Inet6Address.getByName("0:0:0:0:0:0:0:1"), 128));
        assertThat(converter.convertTo(results)).isEqualTo(defaultList);
    }

    @Test
    public void testNormalize() throws Exception {
        final String defaultList = "127.0.0.1/32, ::1/128";
        final String normalized = "127.0.0.1/32,0:0:0:0:0:0:0:1/128";
        final Set<IpSubnet> results = converter.convertFrom(defaultList);
        assertThat(converter.convertTo(results)).isEqualTo(normalized);
    }

    @Test
    public void testNull() throws Exception {
        assertThat(converter.convertFrom(null)).isEmpty();
    }

    @Test
    public void convertFromThrowsParameterExceptionWithInvalidSubnet() {
        expectedException.expect(ParameterException.class);
        expectedException.expectMessage("Invalid subnet: HODOR");
        converter.convertFrom("127.0.0.1/32, ::1/128, HODOR");
    }

    @Test
    public void convertToThrowsParameterExceptionWithNull() {
        expectedException.expect(ParameterException.class);
        expectedException.expectMessage("Couldn't convert IP subnets <null> to string.");
        converter.convertTo(null);
    }
}

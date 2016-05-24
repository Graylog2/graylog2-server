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

import com.google.common.base.Joiner;
import org.jboss.netty.handler.ipfilter.IpSubnet;
import com.github.joschi.jadconfig.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;

/**
 * Converts a comma separated list of IP addresses / subnets to IpSubnet set.
 */
public class IPSubnetConverter implements Converter<Set<IpSubnet>> {
    private static final Logger LOG = LoggerFactory.getLogger(IPSubnetConverter.class);

    public IPSubnetConverter() {
    }

    public Set<IpSubnet> convertFrom(String value) {
        Set<IpSubnet> converted = new HashSet<IpSubnet>();
        if (value instanceof String) {
            String[] subnets = value.split(",");
            for (String subnet: subnets) {
                try {
                    converted.add(new IpSubnet(subnet.trim()));
                } catch (UnknownHostException e) {
                    LOG.error("Invalid subnet {}", subnet);
                }
            }
        }
        return converted;
    }

    public String convertTo(Set<IpSubnet> value) {
        Joiner joiner = Joiner.on(",").skipNulls();
        return joiner.join(value);
    }
}

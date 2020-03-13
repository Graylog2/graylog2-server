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

import com.github.joschi.jadconfig.Converter;
import com.github.joschi.jadconfig.ParameterException;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

import java.net.UnknownHostException;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Converts a comma separated list of IP addresses / sub nets to set of {@link IpSubnet}.
 */
public class IPSubnetConverter implements Converter<Set<IpSubnet>> {
    @Override
    public Set<IpSubnet> convertFrom(String value) {
        final Set<IpSubnet> converted = new LinkedHashSet<>();
        if (value != null) {
            Iterable<String> subnets = Splitter.on(',').trimResults().split(value);
            for (String subnet : subnets) {
                try {
                    converted.add(new IpSubnet(subnet));
                } catch (UnknownHostException e) {
                    throw new ParameterException("Invalid subnet: " + subnet);
                }
            }
        }
        return converted;
    }

    @Override
    public String convertTo(Set<IpSubnet> value) {
        if (value == null) {
            throw new ParameterException("Couldn't convert IP subnets <null> to string.");
        }
        return Joiner.on(",").skipNulls().join(value);
    }
}

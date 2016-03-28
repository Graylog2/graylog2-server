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

import com.atlassian.ip.IPMatcher;
import com.github.joschi.jadconfig.Converter;

/**
 * Converts a comma separated list of IP addresses / subnets to IPMatcher.
 */
public class IPMatcherConverter implements Converter<IPMatcher> {

    public IPMatcherConverter() {
    }

    public IPMatcher convertFrom(String value) {
        IPMatcher.Builder builder = IPMatcher.builder();
        if (value instanceof String) {
            String[] subnets = value.split(",");
            for (String subnet: subnets) {
                // May throw IllegalArgumentException on garbage input
                builder.addPatternOrHost(subnet.trim());
            }

        }
        return builder.build();
    }

    public String convertTo(IPMatcher value) {
        // Does NOT return original configuration value, that is not available
        return value.toString();

    }
}

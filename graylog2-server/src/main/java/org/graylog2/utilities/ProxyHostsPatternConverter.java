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

public class ProxyHostsPatternConverter implements Converter<ProxyHostsPattern> {
    @Override
    public ProxyHostsPattern convertFrom(String value) {
        try {
            return ProxyHostsPattern.create(value);
        } catch (IllegalArgumentException e) {
            throw new ParameterException("Invalid proxy hosts pattern: \"" + value + "\"", e);
        }
    }

    @Override
    public String convertTo(ProxyHostsPattern value) {
        return value.getNoProxyHosts();
    }
}

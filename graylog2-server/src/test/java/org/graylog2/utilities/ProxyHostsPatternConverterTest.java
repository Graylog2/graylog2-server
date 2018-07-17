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

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ProxyHostsPatternConverterTest {
    @Test
    public void convertFromAndTo() {
        final ProxyHostsPatternConverter converter = new ProxyHostsPatternConverter();
        final ProxyHostsPattern pattern = converter.convertFrom("127.0.0.1,node0.graylog.example.com");

        assertThat(pattern.matches("127.0.0.1")).isTrue();
        assertThat(converter.convertTo(pattern)).isEqualTo("127.0.0.1,node0.graylog.example.com");
    }
}
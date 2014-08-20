/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.inputs.converters;

import org.graylog2.plugin.inputs.Converter;
import org.testng.annotations.Test;

import java.util.HashMap;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class IPAnonymizerConverterTest {

    @Test
    public void testConvert() throws Exception {
        Converter hc = new IPAnonymizerConverter(new HashMap<String, Object>());

        assertNull(hc.convert(null));
        assertEquals("", hc.convert(""));
        assertEquals("lol no IP in here", hc.convert("lol no IP in here"));
        assertEquals("127.0.1", hc.convert("127.0.1"));
        assertEquals("127.0.0.xxx", hc.convert("127.0.0.xxx"));

        assertEquals("127.0.0.xxx", hc.convert("127.0.0.1"));
        assertEquals("127.0.0.xxx foobar 192.168.1.xxx test", hc.convert("127.0.0.1 foobar 192.168.1.100 test"));
    }

}

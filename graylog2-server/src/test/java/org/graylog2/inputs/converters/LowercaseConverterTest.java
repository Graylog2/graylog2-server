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
public class LowercaseConverterTest {

    @Test
    public void testConvert() throws Exception {
        Converter c = new LowercaseConverter(new HashMap<String, Object>());

        assertNull(c.convert(null));
        assertEquals("", c.convert(""));
        assertEquals("foobar", c.convert("foobar"));
        assertEquals("foo bar", c.convert("foo BAR"));
        assertEquals("foobar", c.convert("FooBar"));
        assertEquals("foobar ", c.convert("foobar "));
        assertEquals(" foobar", c.convert(" foobar"));
        assertEquals("foobar", c.convert("FOOBAR"));
    }

}

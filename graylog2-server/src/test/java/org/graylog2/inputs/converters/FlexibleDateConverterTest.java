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
import static org.testng.Assert.*;

import java.util.HashMap;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class FlexibleDateConverterTest {

    @Test
    public void testConvert() throws Exception {
        Converter c = new FlexibleDateConverter(new HashMap<String, Object>());

        assertNull(c.convert(null));
        assertEquals(null, c.convert(""));
        assertEquals(null, c.convert("foo"));

        // Using startsWith here to avoid time zone problems in tests.
        assertTrue(c.convert("2014-3-12").toString().startsWith("2014-03-12T"));
        assertTrue(c.convert("2014-3-12 12:27").toString().startsWith("2014-03-12T12:27:00.000"));
        assertTrue(c.convert("Mar 12").toString().startsWith("2014-03-12T"));
        assertTrue(c.convert("Mar 12 2pm").toString().startsWith("2014-03-12T14:00:00.000"));
        assertTrue(c.convert("Mar 12 14:45:38").toString().startsWith("2014-03-12T14:45:38.000"));
        assertTrue(c.convert("Mar 2 13:48:18").toString().startsWith("2014-03-02T13:48:18.000"));
    }

}

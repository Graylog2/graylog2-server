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

import org.graylog2.ConfigurationException;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class SplitAndCountConverterTest {

    @Test
    public void testConvert() throws Exception {
        assertEquals(0, new SplitAndCountConverter(config("x")).convert(""));
        assertEquals(0, new SplitAndCountConverter(config("_")).convert("foo-bar-baz"));
        assertEquals(2, new SplitAndCountConverter(config("-")).convert("foo-bar"));
        assertEquals(3, new SplitAndCountConverter(config("-")).convert("foo-bar-baz"));
        assertEquals(3, new SplitAndCountConverter(config(".")).convert("foo.bar.baz")); // Regex. Must be escaped.
    }

    @Test(expectedExceptions = ConfigurationException.class)
    public void testWithEmptyConfig() throws Exception {
        assertEquals(null, new SplitAndCountConverter(config("")).convert("foo"));
    }

    @Test(expectedExceptions = ConfigurationException.class)
    public void testWithNullConfig() throws Exception {
        assertEquals(null, new SplitAndCountConverter(config(null)).convert("foo"));
    }

    private Map<String, Object> config(final String splitBy) {
        return new HashMap<String, Object>() {{
            put("split_by", splitBy);
        }};
    }

}

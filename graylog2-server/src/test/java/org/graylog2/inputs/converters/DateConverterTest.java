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
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class DateConverterTest {

    @Test
    public void testBasicConvert() throws Exception {
        // .startsWith() because of possibly different timezones per test environment.
        assert(new DateConverter(config("YYYY MMM dd HH:mm:ss")).convert("2013 Aug 15 23:15:16").toString().startsWith("2013-08-15T23:15:16.000"));
    }

    @Test
    public void testAnotherBasicConvert() throws Exception {
        DateTime date = (DateTime) new DateConverter(config("yyyy-MM-dd'T'HH:mm:ss.SSSSSSZZ")).convert("2014-05-19T00:30:43.116847+00:00");
        assertEquals("2014-05-19T00:30:43.116Z", date.toDateTime(DateTimeZone.UTC).toString());
    }

    @Test(expectedExceptions = ConfigurationException.class)
    public void testWithEmptyConfig() throws Exception {
        assertEquals(null, new DateConverter(config("")).convert("foo"));
    }

    @Test(expectedExceptions = ConfigurationException.class)
    public void testWithNullConfig() throws Exception {
        assertEquals(null, new DateConverter(config(null)).convert("foo"));
    }

    private Map<String, Object> config(final String dateFormat) {
        return new HashMap<String, Object>() {{
            put("date_format", dateFormat);
        }};
    }

}

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
package org.graylog2.shared;

import com.github.joschi.jadconfig.ParameterException;
import org.joda.time.Period;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class JadPeriodConverterTest {

    @Test
    public void testConvertFrom() throws Exception {
        final JadPeriodConverter converter = new JadPeriodConverter();

        Exception exc = null;
        try {
            converter.convertFrom("");
        } catch (ParameterException e) {
            exc = e;
        }
        assertNotNull(exc, "empty string should throw exception");

        assertEquals(converter.convertFrom("PT1M"), Period.minutes(1), "IsoFormat is parsed correctly");
        assertEquals(converter.convertFrom("P4DT1M"), Period.days(4).withMinutes(1), "Complex IsoFormat is parsed correctly");

        assertEquals(converter.convertFrom("1d"), Period.days(1), "Simple format is parsed correctly");
        assertEquals(converter.convertFrom("3h"), Period.hours(3), "Simple format is parsed correctly");
        assertEquals(converter.convertFrom("2m"), Period.minutes(2), "Simple format is parsed correctly");
        assertEquals(converter.convertFrom("4s"), Period.seconds(4), "Simple format is parsed correctly");

        exc = null;
        try {
            converter.convertFrom("1M");
        } catch (ParameterException e) {
            exc = e;
        }
        assertNotNull(exc, "Invalid simple format throws exception");
    }

    @Test
    public void testConvertTo() throws Exception {
        final JadPeriodConverter converter = new JadPeriodConverter();
        assertEquals(converter.convertTo(Period.days(1).withHours(4).withMinutes(1)), "P1DT4H1M", "IsoDateFormatter is used");
    }
}
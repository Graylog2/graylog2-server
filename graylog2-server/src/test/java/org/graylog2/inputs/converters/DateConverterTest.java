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
package org.graylog2.inputs.converters;

import org.assertj.jodatime.api.Assertions;
import org.graylog2.ConfigurationException;
import org.graylog2.plugin.inputs.Converter;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.YearMonth;
import org.junit.Test;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class DateConverterTest {
    @Test
    public void testBasicConvert() throws Exception {
        // .startsWith() because of possibly different timezones per test environment.
        final DateConverter converter = new DateConverter(config("YYYY MMM dd HH:mm:ss", null, null));
        final Object result = converter.convert("2013 Aug 15 23:15:16");
        assertThat(result).isNotNull();
        assertThat(String.valueOf(result)).startsWith("2013-08-15T23:15:16.000");
    }

    @Test
    public void testAnotherBasicConvert() throws Exception {
        final DateConverter converter = new DateConverter(config("yyyy-MM-dd'T'HH:mm:ss.SSSSSSZZ", "Etc/UTC", null));
        final DateTime date = (DateTime) converter.convert("2014-05-19T00:30:43.116+00:00");
        assertThat(date).isNotNull();
        Assertions.assertThat(date).isEqualTo(new DateTime(2014, 5, 19, 0, 30, 43, 116, DateTimeZone.UTC));

    }

    @Test
    public void testConvertWithoutYear() throws Exception {
        final int year = YearMonth.now(DateTimeZone.UTC).getYear();
        final DateConverter converter = new DateConverter(config("dd-MM HH:mm:ss", "Etc/UTC", null));
        final DateTime date = (DateTime) converter.convert("19-05 10:20:30");
        assertThat(date).isNotNull();
        Assertions.assertThat(date).isEqualTo(new DateTime(year, 5, 19, 10, 20, 30, DateTimeZone.UTC));
    }

    @Test(expected = ConfigurationException.class)
    public void testWithEmptyDateFormat() throws Exception {
        assertThat(new DateConverter(config("", null, null)).convert("foo")).isNull();
    }

    @Test(expected = ConfigurationException.class)
    public void testWithNullDateFormat() throws Exception {
        assertThat(new DateConverter(config(null, null, null)).convert("foo")).isNull();
    }

    @Test
    public void convertObeysTimeZone() throws Exception {
        final DateTimeZone timeZone = DateTimeZone.forOffsetHours(12);
        final Converter c = new DateConverter(config("YYYY-MM-dd HH:mm:ss", timeZone.toString(), null));

        final DateTime dateOnly = (DateTime) c.convert("2014-03-12 10:00:00");
        assertThat(dateOnly.getZone()).isEqualTo(timeZone);

        Assertions.assertThat(dateOnly)
                .isEqualTo(new DateTime(2014, 3, 12, 10, 0, 0, timeZone))
                .isBefore(new DateTime(2014, 3, 13, 10, 0, 0, timeZone));

        final DateTime dateTime = (DateTime) c.convert("2014-03-12 12:34:00");
        assertThat(dateTime.getZone()).isEqualTo(timeZone);
        Assertions.assertThat(dateTime)
                .isEqualTo(new DateTime(2014, 3, 12, 12, 34, 0, timeZone));
    }

    @Test
    public void convertUsesEtcUTCIfTimeZoneSettingIsEmpty() throws Exception {
        final Converter c = new DateConverter(config("YYYY-MM-dd HH:mm:ss", "", null));
        final DateTime dateOnly = (DateTime) c.convert("2014-03-12 10:00:00");
        assertThat(dateOnly.getZone()).isEqualTo(DateTimeZone.forID("Etc/UTC"));
    }

    @Test
    public void convertUsesEtcUTCIfTimeZoneSettingIsBlank() throws Exception {
        final Converter c = new DateConverter(config("YYYY-MM-dd HH:mm:ss", " ", null));
        final DateTime dateOnly = (DateTime) c.convert("2014-03-12 10:00:00");
        assertThat(dateOnly.getZone()).isEqualTo(DateTimeZone.forID("Etc/UTC"));
    }

    @Test
    public void convertUsesEtcUTCIfTimeZoneSettingIsInvalid() throws Exception {
        final Converter c = new DateConverter(config("YYYY-MM-dd HH:mm:ss", "TEST", null));
        final DateTime dateOnly = (DateTime) c.convert("2014-03-12 10:00:00");
        assertThat(dateOnly.getZone()).isEqualTo(DateTimeZone.forID("Etc/UTC"));
    }

    @Test
    public void convertUsesEnglishIfLocaleIsNull() throws Exception {
        final Converter c = new DateConverter(config("dd/MMM/YYYY HH:mm:ss Z", null, null));
        final DateTime dateOnly = (DateTime) c.convert("11/May/2017 15:10:48 +0200");
        Assertions.assertThat(dateOnly).isEqualTo(new DateTime(2017, 5, 11, 13, 10, 48, DateTimeZone.UTC));
    }

    @Test
    public void convertUsesEnglishIfLocaleIsInvalid() throws Exception {
        final Converter c = new DateConverter(config("dd/MMM/YYYY HH:mm:ss Z", null, "Wurstweck"));
        final DateTime dateOnly = (DateTime) c.convert("11/May/2017 15:10:48 +0200");
        Assertions.assertThat(dateOnly).isEqualTo(new DateTime(2017, 5, 11, 13, 10, 48, DateTimeZone.UTC));
    }

    @Test
    public void convertUsesCustomLocale() throws Exception {
        final Converter c = new DateConverter(config("dd/MMM/YYYY HH:mm:ss Z", null, "de-DE"));
        final DateTime dateOnly = (DateTime) c.convert("11/März/2017 15:10:48 +0200");
        Assertions.assertThat(dateOnly).isEqualTo(new DateTime(2017, 3, 11, 13, 10, 48, DateTimeZone.UTC));
    }

    /**
     * Test case for <a href="https://github.com/Graylog2/graylog2-server/issues/2648">#2648</a>.
     */
    @Test
    public void issue2648() throws Exception {
        final Converter utc = new DateConverter(config("YYYY-MM-dd HH:mm:ss", "UTC", null));
        final DateTime utcDate = (DateTime) utc.convert("2016-08-10 12:00:00");
        assertThat(utcDate.getZone()).isEqualTo(DateTimeZone.UTC);
        assertThat(utcDate.getZone().getOffsetFromLocal(0L)).isEqualTo(0);

        final Converter cet = new DateConverter(config("YYYY-MM-dd HH:mm:ss", "CET", null));
        final DateTime cetDate = (DateTime) cet.convert("2016-08-10 12:00:00");
        assertThat(cetDate.getZone()).isEqualTo(DateTimeZone.forID("CET"));
        assertThat(cetDate.getZone().getOffsetFromLocal(0L)).isEqualTo((int) Duration.standardHours(1L).getMillis());

        final Converter berlin = new DateConverter(config("YYYY-MM-dd HH:mm:ss", "Europe/Berlin", null));
        final DateTime berlinDate = (DateTime) berlin.convert("2016-08-10 12:00:00");
        assertThat(berlinDate.getZone()).isEqualTo(DateTimeZone.forID("Europe/Berlin"));
        assertThat(berlinDate.getZone().getOffsetFromLocal(0L)).isEqualTo((int) Duration.standardHours(1L).getMillis());
    }

    private Map<String, Object> config(final String dateFormat, final String timeZone, final String locale) {
        final Map<String, Object> config = new HashMap<>();
        config.put("date_format", dateFormat);
        config.put("time_zone", timeZone);
        config.put("locale", locale);
        return config;
    }
}

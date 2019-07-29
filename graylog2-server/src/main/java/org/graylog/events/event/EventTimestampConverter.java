package org.graylog.events.event;

import com.fasterxml.jackson.databind.util.StdConverter;
import org.graylog2.plugin.Tools;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

import static org.joda.time.DateTimeZone.UTC;

/**
 * Converts our Elasticsearch timestamp format into a {@link DateTime} object.
 */
public class EventTimestampConverter extends StdConverter<String, DateTime> {
    @Override
    public DateTime convert(String value) {
        // TODO: This throws an exception with a weird date format when we try to use a DTO that is using the
        //       converter. Check DBJobTriggerService#create, there is an IllegalArgumentException when
        //       getSavedObject() is called.
        //       To reproduce this: Add this converter to the timestamps in EventDto and trigger a notification.
        //   IDEA: We probably get a Date value here? because of mongodb
        // If the date string contains a "T" character, it's most probably a regular ISO8601 formatted timestamp and
        // we can use the standard parser.
        if (value != null && value.contains("T")) {
            return DateTime.parse(value, ISODateTimeFormat.dateTimeParser().withZone(UTC));
        }
        return DateTime.parse(value, Tools.timeFormatterWithOptionalMilliseconds().withZone(UTC));
    }
}

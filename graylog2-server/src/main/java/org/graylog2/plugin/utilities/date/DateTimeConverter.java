package org.graylog2.plugin.utilities.date;

import org.joda.time.DateTime;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;

import static org.graylog2.plugin.Tools.ES_DATE_FORMAT_FORMATTER;

public class DateTimeConverter {
    public static DateTime convertToDateTime(@Nonnull Object value) {
        if (value instanceof DateTime) {
            return (DateTime) value;
        }

        if (value instanceof Date) {
            return new DateTime(value);
        } else if (value instanceof ZonedDateTime) {
            return new DateTime(Date.from(((ZonedDateTime) value).toInstant()));
        } else if (value instanceof OffsetDateTime) {
            return new DateTime(Date.from(((OffsetDateTime) value).toInstant()));
        } else if (value instanceof LocalDateTime) {
            final LocalDateTime localDateTime = (LocalDateTime) value;
            final ZoneId defaultZoneId = ZoneId.systemDefault();
            final ZoneOffset offset = defaultZoneId.getRules().getOffset(localDateTime);
            return new DateTime(Date.from(localDateTime.toInstant(offset)));
        } else if (value instanceof LocalDate) {
            final LocalDate localDate = (LocalDate) value;
            final LocalDateTime localDateTime = localDate.atStartOfDay();
            final ZoneId defaultZoneId = ZoneId.systemDefault();
            final ZoneOffset offset = defaultZoneId.getRules().getOffset(localDateTime);
            return new DateTime(Date.from(localDateTime.toInstant(offset)));
        } else if (value instanceof Instant) {
            return new DateTime(Date.from((Instant) value));
        } else if (value instanceof String) {
            return ES_DATE_FORMAT_FORMATTER.parseDateTime((String) value);
        } else {
            throw new IllegalArgumentException("Value of invalid type <" + value.getClass().getSimpleName() + "> provided");
        }
    }
}

/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.utilities.date;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.plugin.Tools.ES_DATE_FORMAT_FORMATTER;
import static org.graylog2.plugin.Tools.ES_DATE_FORMAT_NO_MS_FORMATTER;
import static org.graylog2.plugin.Tools.ISO_DATE_FORMAT_FORMATTER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class MultiFormatDateParserTest {

    private MultiFormatDateParser toTest;

    @Test
    void throwsExceptionIfThereIsNoFormatter() {
        toTest = new MultiFormatDateParser(List.of());
        assertThrows(IllegalArgumentException.class, () -> toTest.parseDate("2012-12-12 12:12:12"));
    }

    @Test
    void throwsExceptionIfThereIsNoSuitableFormatter() {
        toTest = new MultiFormatDateParser(List.of(mockWrongFormatter(), mockWrongFormatter()));
        assertThrows(IllegalArgumentException.class, () -> toTest.parseDate("2012-12-12 12:12:12"));
    }

    @Test
    void usesFirstGoodFormatter() {
        final DateTime date1 = new DateTime(2012, 12, 12, 12, 12, 12, DateTimeZone.UTC);
        final DateTime date2 = new DateTime(2012, 12, 12, 12, 12, 12, 42, DateTimeZone.UTC);
        final DateTimeFormatter formatter1 = mockGoodFormatter(date1);
        final DateTimeFormatter formatter2 = mockGoodFormatter(date2);
        toTest = new MultiFormatDateParser(List.of(
                mockWrongFormatter(),
                mockWrongFormatter(),
                formatter1,
                formatter2
        ));
        final DateTime returnedDate = toTest.parseDate("2012-12-12 12:12:12");
        assertEquals(date1, returnedDate);
        verify(formatter1).parseDateTime("2012-12-12 12:12:12");
        verifyNoInteractions(formatter2);
    }

    @Test
    void worksWithDefaultListOfFormatters() {
        toTest = new MultiFormatDateParser();

        DateTime returnedDate = toTest.parseDate("2012-12-12 12:12:12");
        assertThat(returnedDate)
                .isNotNull()
                .isEqualTo(ES_DATE_FORMAT_NO_MS_FORMATTER.parseDateTime("2012-12-12 12:12:12"));

        returnedDate = toTest.parseDate("2012-12-12 12:12:12.123");
        assertThat(returnedDate)
                .isNotNull()
                .isEqualTo(ES_DATE_FORMAT_FORMATTER.parseDateTime("2012-12-12 12:12:12.123"));

        returnedDate = toTest.parseDate("2023-02-16T10:31:38.123Z");
        assertThat(returnedDate)
                .isNotNull()
                .isEqualTo(ISO_DATE_FORMAT_FORMATTER.parseDateTime("2023-02-16T10:31:38.123Z"));

    }

    private DateTimeFormatter mockGoodFormatter(final DateTime returnedDateTime) {
        DateTimeFormatter formatter = mock(DateTimeFormatter.class);
        doReturn(returnedDateTime).when(formatter).parseDateTime(anyString());
        return formatter;
    }

    private DateTimeFormatter mockWrongFormatter() {
        DateTimeFormatter formatter = mock(DateTimeFormatter.class);
        doThrow(new IllegalArgumentException("I don't like this date format")).when(formatter).parseDateTime(anyString());
        return formatter;
    }
}

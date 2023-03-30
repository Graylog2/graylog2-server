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
package org.graylog.plugins.views.search.rest.scriptingapi.parsing;

import org.graylog2.plugin.indexer.searches.timeranges.KeywordRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class TimerangeParserTest {

    @InjectMocks
    private TimerangeParser toTest;
    @Mock
    private ShortTimerangeFormatParser shortTimerangeFormatParser;

    @Test
    void returnsNullOnBlankTimerange() {
        assertNull(toTest.parseTimeRange(null));
        assertNull(toTest.parseTimeRange(""));
        assertNull(toTest.parseTimeRange("  "));
    }

    @Test
    void throwsExceptionOnInputImpossibleToParse() {
        doReturn(Optional.empty()).when(shortTimerangeFormatParser).parse("Guadalajara!");
        assertThrows(IllegalArgumentException.class, () -> toTest.parseTimeRange("Guadalajara!"));
    }

    @Test
    void returnsShortTimerangeParserResponseIfItIsNotEmpty() {
        doReturn(Optional.of(KeywordRange.create("last 1 year", "UTC"))).when(shortTimerangeFormatParser).parse("1y");
        final TimeRange result = toTest.parseTimeRange("1y");
        assertEquals(KeywordRange.create("last 1 year", "UTC"), result);
    }

    @Test
    void buildsKeywordTimerangeIfInputIsNotInShortFormat() {
        doReturn(Optional.empty()).when(shortTimerangeFormatParser).parse("last 42 years");
        final TimeRange result = toTest.parseTimeRange("last 42 years");
        assertEquals(KeywordRange.create("last 42 years", "UTC"), result);
    }
}

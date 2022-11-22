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

import javax.inject.Inject;
import java.util.Optional;

//TODO: unit tests + error handling
public class TimerangeParser {

    private final ShortTimerangeFormatParser shortTimerangeFormatParser;

    @Inject
    public TimerangeParser(ShortTimerangeFormatParser shortTimerangeFormatParser) {
        this.shortTimerangeFormatParser = shortTimerangeFormatParser;
    }

    public TimeRange parseTimeRange(final String timerangeKeyword) {
        if (timerangeKeyword == null) {
            return null;
        }
        final Optional<TimeRange> shortTimeRange = shortTimerangeFormatParser.parse(timerangeKeyword);
        return shortTimeRange.orElseGet(() -> KeywordRange.create(timerangeKeyword, "UTC"));
    }
}

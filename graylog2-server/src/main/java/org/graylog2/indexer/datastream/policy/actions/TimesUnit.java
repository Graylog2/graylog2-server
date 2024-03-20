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
package org.graylog2.indexer.datastream.policy.actions;

import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * Formats time values using accepted OpenSearch values.
 * <a href="https://opensearch.org/docs/latest/api-reference/units/">...</a>
 */
public enum TimesUnit {
    DAYS("d"),
    HOURS("h"),
    MINUTES("m"),
    SECONDS("s"),
    MILLISECONDS("ms"),
    MICROSECONDS("micros"),
    NANOSECONDS("nanos");

    private final String abbreviation;

    TimesUnit(String abbreviation) {
        this.abbreviation = abbreviation;
    }

    public String format(long size) {
        return f("%d" + this.abbreviation, size);
    }
}

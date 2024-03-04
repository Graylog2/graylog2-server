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

public class RolloverActionFormatter {

    private RolloverActionFormatter() {
    }

    /**
     * Formats retention value in days using the proper OpenSearch time value format (e.g. 14d).
     * <a href="https://opensearch.org/docs/latest/im-plugin/ism/policies/#transitions">...</a>
     */
    public static String formatDaysDuration(long days) {
        return f("%dd", days);
    }

    /**
     * Formats size value in gigabytes using the proper OpenSearch size value format (e.g. 30gb).
     * <a href="https://opensearch.org/docs/latest/im-plugin/ism/policies/#transitions">...</a>
     */
    public static String formatGbSize(long gigabytes) {
        return f("%dgb", gigabytes);
    }
}

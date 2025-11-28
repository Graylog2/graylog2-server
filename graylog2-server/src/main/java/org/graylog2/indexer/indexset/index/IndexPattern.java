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
package org.graylog2.indexer.indexset.index;

import java.util.regex.Pattern;

public class IndexPattern {

    public static final String SEPARATOR = "_";
    // TODO: Hardcoded archive suffix. See: https://github.com/Graylog2/graylog2-server/issues/2058
    // TODO 3.0: Remove this in 3.0, only used for pre 2.2 backwards compatibility.
    public static final String RESTORED_ARCHIVE_SUFFIX = "_restored_archive";
    public static final String WARM_INDEX_INFIX = "warm_";
    private final Pattern index;
    private final Pattern deflector;

    public IndexPattern(String pattern) {
        this.index = Pattern.compile("^" + pattern + SEPARATOR + "(?:" + WARM_INDEX_INFIX + ")?" + "\\d+(?:" + RESTORED_ARCHIVE_SUFFIX + ")?");
        this.deflector = Pattern.compile("^" + pattern + SEPARATOR + "(?:" + WARM_INDEX_INFIX + ")?" + "\\d+");
    }

    public boolean indexMatches(String indexName) {
        return index.matcher(indexName).matches();
    }

    public boolean deflectorIndexMatches(String indexName) {
        return deflector.matcher(indexName).matches();
    }
}

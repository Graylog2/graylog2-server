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
package org.graylog.plugins.views.search.engine;

public class QueryFragment {
    private final String originalContent;
    private final String interpolatedContent;
    private final int line;
    private final int originalBeginColumn;
    private final int originalEndColumn;

    public QueryFragment(String originalContent, String interpolatedContent, int line, int originalBeginColumn, int originalEndColumn) {
        this.originalContent = originalContent;
        this.interpolatedContent = interpolatedContent;
        this.line = line;
        this.originalBeginColumn = originalBeginColumn;
        this.originalEndColumn = originalEndColumn;
    }

    public String getOriginalContent() {
        return originalContent;
    }

    public String getInterpolatedContent() {
        return interpolatedContent;
    }

    public int getLine() {
        return line;
    }

    public int getOriginalBeginColumn() {
        return originalBeginColumn;
    }

    public int getOriginalEndColumn() {
        return originalEndColumn;
    }
}

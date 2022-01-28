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
package org.graylog.plugins.views.search.validation;

import java.util.ArrayList;
import java.util.List;

public class SubstringMultilinePosition {
    private final int line;
    private final int columnStart;
    private final int columnEnd;

    private SubstringMultilinePosition(int line, int columnStart, int columnEnd) {
        this.line = line;
        this.columnStart = columnStart;
        this.columnEnd = columnEnd;
    }

    public static List<SubstringMultilinePosition> compute(final String input, final String substring) {
        final List<SubstringMultilinePosition> positions = new ArrayList<>();
        final String[] lines = input.split("\n");
        for (int line = 0; line < lines.length; line++) {
            int startPosition = 0;
            while ((startPosition = lines[line].indexOf(substring, startPosition)) > 0) {
                final int endPosition = startPosition + substring.length();
                positions.add(new SubstringMultilinePosition(line + 1, startPosition, endPosition));
                startPosition = endPosition;
            }

        }
        return positions;
    }

    public int getLine() {
        return line;
    }

    public int getBeginColumn() {
        return columnStart;
    }

    public int getEndColumn() {
        return columnEnd;
    }
}

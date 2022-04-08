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

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PositionTrackingQuery {
    private final List<QueryFragment> fragments;

    public PositionTrackingQuery(List<QueryFragment> fragments) {
        this.fragments = fragments;
    }

    public static PositionTrackingQuery of(String queryString) {
        return new PositionTrackingQuery(Collections.singletonList(
                new QueryFragment(queryString, queryString, 1, 0, queryString.length())
        ));
    }

    public List<QueryFragment> getFragments() {
        return fragments;
    }

    public String getOriginalQuery() {
        return concatFragments(QueryFragment::getOriginalContent);
    }

    public String getInterpolatedQuery() {
        return concatFragments(QueryFragment::getInterpolatedContent);
    }

    private String concatFragments(Function<QueryFragment, String> supplier) {
        int line = 1;
        final StringBuilder stringBuilder = new StringBuilder();
        for (QueryFragment fragment : fragments) {
            if (line != fragment.getLine()) {
                stringBuilder.append("\n");
                line = fragment.getLine();
            }
            stringBuilder.append(supplier.apply(fragment));
        }
        return stringBuilder.toString();
    }

    public Optional<QueryPosition> backtrackPosition(int line, int interpolatedBeginColumn, int interpolatedEndColumn) {
        final List<QueryFragment> lineFragments = this.fragments.stream().filter(f -> f.getLine() == line).collect(Collectors.toList());
        int linePosition = 0;
        for (QueryFragment fragment : lineFragments) {
            if (interpolatedBeginColumn >= linePosition && interpolatedEndColumn <= linePosition + fragment.originalLength()) {
                if (fragment.isInterpolated()) { // we can't map 1:1 interpolated and original positions, let's use the whole fragment
                    return Optional.of(new QueryPosition(fragment.getLine(), fragment.getOriginalBeginColumn(), fragment.getOriginalEndColumn()));
                } else { // we can map exactly the positions
                    final int offsetStart = interpolatedBeginColumn - linePosition;
                    final int offsetEnd = linePosition + fragment.interpolatedLength() - interpolatedEndColumn;
                    final int globalOriginalStart = fragment.getOriginalBeginColumn() + offsetStart;
                    final int globalOriginalEnd = fragment.getOriginalBeginColumn() + fragment.originalLength() - offsetEnd;
                    return Optional.of(new QueryPosition(line, globalOriginalStart, globalOriginalEnd));
                }
            }
            linePosition = linePosition + fragment.interpolatedLength();
        }
        return Optional.empty();
    }
}

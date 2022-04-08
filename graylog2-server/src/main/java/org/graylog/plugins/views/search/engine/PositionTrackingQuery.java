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
import java.util.function.Function;

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
        for (QueryFragment fragment: fragments) {
            if(line != fragment.getLine()) {
                stringBuilder.append("\n");
                line = fragment.getLine();
            }
            stringBuilder.append(supplier.apply(fragment));
        }
        return stringBuilder.toString();
    }

}

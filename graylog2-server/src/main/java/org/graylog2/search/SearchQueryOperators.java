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
package org.graylog2.search;

public class SearchQueryOperators {
    public static final SearchQueryOperator EQUALS = new SearchQueryOperator.Equals();
    public static final SearchQueryOperator GREATER = new SearchQueryOperator.Greater();
    public static final SearchQueryOperator GREATER_EQUALS = new SearchQueryOperator.GreaterEquals();
    public static final SearchQueryOperator LESS = new SearchQueryOperator.Less();
    public static final SearchQueryOperator LESS_EQUALS = new SearchQueryOperator.LessEquals();
    public static final SearchQueryOperator REGEXP = new SearchQueryOperator.Regexp();
}

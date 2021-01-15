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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SearchQueryOperatorTest {
    @Nested
    class RegexpTest {
        private SearchQueryOperator.Regexp operator;

        @BeforeEach
        void setUp() {
            this.operator = new SearchQueryOperator.Regexp();
        }

        @Test
        void withRegexpMetaCharacters() {
            // Using regexp meta characters should now throw an exception
            operator.buildQuery("hello", "foo\\");
        }
    }
}

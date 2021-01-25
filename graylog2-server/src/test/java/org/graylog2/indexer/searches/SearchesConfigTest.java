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
package org.graylog2.indexer.searches;

import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SearchesConfigTest {

    @Test
    public void defaultLimit() throws InvalidRangeParametersException {
        final SearchesConfig config = SearchesConfig.builder()
                .query("")
                .range(RelativeRange.create(5))
                .limit(0)
                .offset(0)
                .build();

        assertEquals("Limit should default", SearchesConfig.DEFAULT_LIMIT, config.limit());
    }

    @Test
    public void negativeLimit() throws InvalidRangeParametersException {
        final SearchesConfig config = SearchesConfig.builder()
                .query("")
                .range(RelativeRange.create(5))
                .limit(-100)
                .offset(0)
                .build();

        assertEquals("Limit should default", SearchesConfig.DEFAULT_LIMIT, config.limit());
    }

    @Test
    public void explicitLimit() throws InvalidRangeParametersException {
        final SearchesConfig config = SearchesConfig.builder()
                .query("")
                .range(RelativeRange.create(5))
                .limit(23)
                .offset(0)
                .build();

        assertEquals("Limit should not default", 23, config.limit());
    }

}
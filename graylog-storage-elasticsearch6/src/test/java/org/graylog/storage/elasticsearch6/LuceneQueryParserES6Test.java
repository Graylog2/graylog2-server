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
package org.graylog.storage.elasticsearch6;

import org.graylog.plugins.views.search.engine.LuceneQueryParser;
import org.graylog.plugins.views.search.engine.LuceneQueryParsingException;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class LuceneQueryParserES6Test {

    private final LuceneQueryParser parser = new LuceneQueryParserES6();

    @Test
    void getFieldNames() throws LuceneQueryParsingException {
        final Set<String> parserFieldNames = parser.getFieldNames("foo:bar AND lorem:ipsum");
        assertThat(parserFieldNames).contains("foo", "lorem");
    }
}

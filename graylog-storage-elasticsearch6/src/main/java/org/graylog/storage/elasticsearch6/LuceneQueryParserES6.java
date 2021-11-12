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
import org.graylog.shaded.elasticsearch6.org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.graylog.shaded.elasticsearch6.org.apache.lucene.queryparser.classic.ParseException;
import org.graylog.shaded.elasticsearch6.org.apache.lucene.queryparser.classic.QueryParser;
import org.graylog.shaded.elasticsearch6.org.apache.lucene.search.Query;

import java.util.Collections;
import java.util.Set;

public class LuceneQueryParserES6 implements LuceneQueryParser {

    private final QueryParser parser;

    public LuceneQueryParserES6() {
        this.parser = new QueryParser("f", new StandardAnalyzer());
    }

    @Override
    public Set<String> getFieldNames(String query) throws LuceneQueryParsingException {
        final Query parsed;
        try {
            parsed = parser.parse(query);
        } catch (ParseException e) {
            throw new LuceneQueryParsingException(e);
        }
        return Collections.emptySet();
    }
}

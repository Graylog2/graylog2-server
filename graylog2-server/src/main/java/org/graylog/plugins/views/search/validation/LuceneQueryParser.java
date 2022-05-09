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

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Query;

public class LuceneQueryParser {

    public static final StandardAnalyzer ANALYZER = new StandardAnalyzer();

    public ParsedQuery parse(final String query) throws ParseException {
        final TokenCollectingQueryParser parser = new TokenCollectingQueryParser(ParsedTerm.DEFAULT_FIELD, ANALYZER);
        parser.setSplitOnWhitespace(true);

        final Query parsed = parser.parse(query);
        final ParsedQuery.Builder builder = ParsedQuery.builder().query(query);

        builder.tokensBuilder().addAll(parser.getTokens());

        final TermCollectingQueryVisitor visitor = new TermCollectingQueryVisitor(ANALYZER, parser.getTokenLookup());
        parsed.visit(visitor);
        builder.termsBuilder().addAll(visitor.getParsedTerms());
        return builder.build();
    }
}

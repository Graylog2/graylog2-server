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

import javax.inject.Inject;
import javax.inject.Named;

public class LuceneQueryParser {
    private final TermCollectingQueryParser parser;

    @Inject
    public LuceneQueryParser(@Named("allow_leading_wildcard_searches") final boolean allowLeadingWildcard) {
        this.parser = new TermCollectingQueryParser(ParsedTerm.DEFAULT_FIELD, new StandardAnalyzer());
        this.parser.setSplitOnWhitespace(true);
        this.parser.setAllowLeadingWildcard(allowLeadingWildcard);
    }

    public ParsedQuery parse(final String query) throws ParseException {
        final Query parsed = parser.parse(query);
        final ParsedQuery.Builder builder = ParsedQuery.builder().query(query);

        builder.tokensBuilder().addAll(this.parser.getTokens());
        final TermCollectingQueryVisitor visitor = new TermCollectingQueryVisitor(this.parser.getTokens());
        parsed.visit(visitor);
        builder.termsBuilder().addAll(visitor.getParsedTerms());
        return builder.build();
    }
}

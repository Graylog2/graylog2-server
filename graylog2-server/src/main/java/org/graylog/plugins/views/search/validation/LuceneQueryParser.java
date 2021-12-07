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
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParserConstants;
import org.apache.lucene.queryparser.classic.Token;
import org.apache.lucene.search.AutomatonQuery;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryVisitor;

import java.util.List;

public class LuceneQueryParser {
    private final TermCollectingQueryParser parser;

    public LuceneQueryParser() {
        this.parser = new TermCollectingQueryParser(ParsedTerm.DEFAULT_FIELD, new StandardAnalyzer());
    }

    public ParsedQuery parse(final String query) throws ParseException {
        final Query parsed = parser.parse(query);
        final ParsedQuery.Builder builder = ParsedQuery.builder().query(query);

        final List<Token> tokens = this.parser.getTokens();
        builder.tokensBuilder().addAll(tokens);

        parsed.visit(new QueryVisitor() {
            @Override
            public void consumeTerms(Query query, Term... terms) {
                super.consumeTerms(query, terms);
                for (Term t : terms) {
                    final String field = t.field();

                    final ParsedTerm.Builder termBuilder = ParsedTerm.builder()
                            .field(field)
                            .value(t.text());

                    if (field.equals(ParsedTerm.DEFAULT_FIELD)) {
                        tokens.stream()
                                .filter(token -> token.kind == QueryParserConstants.TERM)
                                .filter(token -> token.image.equals(t.text()))
                                .findFirst()
                                .ifPresent(token -> {
                                    termBuilder.tokensBuilder().add(token);
                                });
                    } else {
                        tokens.stream()
                                .filter(token -> token.kind == QueryParserConstants.TERM)
                                .filter(token -> token.image.equals(field))
                                .findFirst()
                                .ifPresent(token -> {
                                    termBuilder.tokensBuilder().add(token);
                                });
                    }
                    builder.termsBuilder().add(termBuilder.build());
                }
            }

            @Override
            public void visitLeaf(Query query) {
                if (query instanceof AutomatonQuery) {
                    final String field = ((AutomatonQuery) query).getField();
                    builder.termsBuilder().add(ParsedTerm.create(ParsedTerm.EXISTS, field));
                }
            }

            @Override
            public QueryVisitor getSubVisitor(BooleanClause.Occur occur, Query parent) {
                // the default implementation ignores MUST_NOT clauses, we want to collect all, even MUST_NOT
                return this;
            }
        });
        return builder.build();
    }
}

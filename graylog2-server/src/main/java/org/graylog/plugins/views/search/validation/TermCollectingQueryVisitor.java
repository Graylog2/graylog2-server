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

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParserConstants;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryVisitor;
import org.apache.lucene.search.RegexpQuery;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.search.WildcardQuery;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TermCollectingQueryVisitor extends QueryVisitor {

    private final Analyzer analyzer;
    private final List<ParsedTerm> parsedTerms = new ArrayList<>();
    private final Map<Query, Collection<ImmutableToken>> tokenLookup;

    public TermCollectingQueryVisitor(Analyzer analyzer, Map<Query, Collection<ImmutableToken>> tokenLookup) {
        this.analyzer = analyzer;
        this.tokenLookup = tokenLookup;
    }

    @Override
    public void consumeTerms(Query query, Term... terms) {
        super.consumeTerms(query, terms);
        final Collection<ImmutableToken> tokens = tokenLookup.getOrDefault(query, Collections.emptySet());
        processTerms(tokens, terms);
    }

    private void processTerms(Collection<ImmutableToken> tokens, Term... terms) {
        for (Term t : terms) {

            final ParsedTerm.Builder termBuilder = ParsedTerm.builder()
                    .field(t.field())
                    .value(t.text());

            if (t.field().equals(ParsedTerm.DEFAULT_FIELD) || t.field().equals(ParsedTerm.EXISTS)) {
                tokens.stream()
                        .filter(token -> token.matches(QueryParserConstants.TERM, t.text()))
                        .findFirst()
                        .ifPresent(termBuilder::keyToken);
            } else {
                tokens.stream()
                        .filter(token -> token.kind() == QueryParserConstants.TERM)
                        .filter(token -> token.image().equals(t.field()))
                        .findFirst()
                        .ifPresent(token -> {
                            termBuilder.keyToken(token);
                            final String value = t.text();
                            tokens.stream()
                                    .filter(v -> v.kind() == QueryParserConstants.TERM)
                                    .filter(v -> normalize(t.field(), v.image()).equals(value))
                                    .findFirst()
                                    .ifPresent(termBuilder::valueToken);


                        });
            }
            parsedTerms.add(termBuilder.build());
        }
    }

    /**
     * To be able to compare token values with query values, we first need to normalize the value, using the same analyzer
     * For example using the StandardAnalyzer, it could mean difference like lowercase conversion
     */
    private String normalize(String fieldName, String value) {
        return analyzer.normalize(fieldName, value).utf8ToString();
    }

    @Override
    public void visitLeaf(Query query) {
        final Collection<ImmutableToken> tokens = tokenLookup.get(query);
        if (query instanceof RegexpQuery) {
            processTerms(tokens, ((RegexpQuery) query).getRegexp());
        } else if (query instanceof TermRangeQuery) { // add lower and upper term as independent values, good enough for validation
            final TermRangeQuery trq = (TermRangeQuery) query;
            processTerms(
                    tokens, new Term(trq.getField(), trq.getLowerTerm().utf8ToString()),
                    new Term(trq.getField(), trq.getUpperTerm().utf8ToString())
            );
        } else if (query instanceof WildcardQuery) {
            processTerms(tokens, ((WildcardQuery) query).getTerm());
        } else if (query instanceof PrefixQuery) {
            processTerms(tokens, ((PrefixQuery) query).getPrefix());
        } else if (query instanceof FuzzyQuery) {
            processTerms(tokens, ((FuzzyQuery) query).getTerm());
        } else {
            throw new IllegalArgumentException("Unrecognized query type: " + query.getClass().getName());
        }
    }

    @Override
    public QueryVisitor getSubVisitor(BooleanClause.Occur occur, Query parent) {
        // the default implementation ignores MUST_NOT clauses, we want to collect all, even MUST_NOT
        return this;
    }

    public List<ParsedTerm> getParsedTerms() {
        return parsedTerms;
    }
}

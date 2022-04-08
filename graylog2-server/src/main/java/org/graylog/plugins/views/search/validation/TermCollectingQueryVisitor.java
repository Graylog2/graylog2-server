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

import com.google.common.collect.Streams;
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
import java.util.List;
import java.util.stream.Stream;

public class TermCollectingQueryVisitor extends QueryVisitor {

    private final List<ImmutableToken> availableTokens;
    private final List<ImmutableToken> processedTokens = new ArrayList<>();
    private final List<ParsedTerm> parsedTerms = new ArrayList<>();

    public TermCollectingQueryVisitor(List<ImmutableToken> availableTokens) {
        this.availableTokens = new ArrayList<>(availableTokens);
    }

    @Override
    public void consumeTerms(Query query, Term... terms) {
        super.consumeTerms(query, terms);
        processTerms(terms);
    }

    private void processTerms(Term... terms) {
        for (Term t : terms) {

            final ParsedTerm.Builder termBuilder = ParsedTerm.builder()
                    .field(t.field())
                    .value(t.text());

            if (t.field().equals(ParsedTerm.DEFAULT_FIELD) || t.field().equals(ParsedTerm.EXISTS)) {
                streamOf(availableTokens, processedTokens)
                        .filter(token -> token.matches(QueryParserConstants.TERM, t.text()))
                        .findFirst()
                        .ifPresent(token -> {
                            termBuilder.keyToken(token);
                            processedTokens.add(token);
                            availableTokens.remove(token);
                        });
            } else {
                streamOf(availableTokens, processedTokens)
                        .filter(token -> token.kind() == QueryParserConstants.TERM)
                        .filter(token -> token.image().equals(t.field()))
                        .findFirst()
                        .ifPresent(token -> {
                            termBuilder.keyToken(token);
                            processedTokens.add(token);
                            availableTokens.remove(token);
                        });
            }
            parsedTerms.add(termBuilder.build());
        }
    }

    @Override
    public void visitLeaf(Query query) {
        if (query instanceof RegexpQuery) {
            processTerms(((RegexpQuery) query).getRegexp());
        } else if (query instanceof TermRangeQuery) { // add lower and upper term as independent values, good enough for validation
            final TermRangeQuery trq = (TermRangeQuery) query;
            processTerms(
                    new Term(trq.getField(), trq.getLowerTerm().utf8ToString()),
                    new Term(trq.getField(), trq.getUpperTerm().utf8ToString())
            );
        } else if (query instanceof WildcardQuery) {
            processTerms(((WildcardQuery) query).getTerm());
        } else if (query instanceof PrefixQuery) {
            processTerms(((PrefixQuery) query).getPrefix());
        } else if (query instanceof FuzzyQuery) {
            processTerms(((FuzzyQuery) query).getTerm());
        } else {
            throw new IllegalArgumentException("Unrecognized query type: " + query.getClass().getName());
        }
    }

    @Override
    public QueryVisitor getSubVisitor(BooleanClause.Occur occur, Query parent) {
        // the default implementation ignores MUST_NOT clauses, we want to collect all, even MUST_NOT
        return this;
    }

    /**
     * One stream consisting of two lists. First unused, fresh tokens. If no match found there, we can fallback to the
     * already processed tokens and find a match there.
     */
    private Stream<ImmutableToken> streamOf(List<ImmutableToken> availableTokens, List<ImmutableToken> processedTokens) {
        return Streams.concat(availableTokens.stream(), processedTokens.stream());
    }

    public List<ParsedTerm> getParsedTerms() {
        return parsedTerms;
    }
}

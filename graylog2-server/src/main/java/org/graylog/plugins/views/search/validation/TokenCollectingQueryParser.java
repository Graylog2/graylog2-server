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

import org.apache.commons.collections4.CollectionUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;

import java.util.Collection;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TokenCollectingQueryParser extends QueryParser {

    private final CollectingQueryParserTokenManager tokenManager;
    private final Set<ImmutableToken> processedTokens = new HashSet<>();
    private final Map<Query, Collection<ImmutableToken>> tokenLookup = new IdentityHashMap<>();

    public TokenCollectingQueryParser(String defaultFieldName, Analyzer analyzer) {
        this(new CollectingQueryParserTokenManager(defaultFieldName, analyzer), defaultFieldName, analyzer);
    }

    private TokenCollectingQueryParser(CollectingQueryParserTokenManager collectingQueryParserTokenManager, String defaultFieldName, Analyzer analyzer) {
        super(collectingQueryParserTokenManager);
        this.tokenManager = collectingQueryParserTokenManager;
        this.init(defaultFieldName, analyzer);
    }


    public List<ImmutableToken> getTokens() {
        return tokenManager.getTokens();
    }

    public Map<Query, Collection<ImmutableToken>> getTokenLookup() {
        return tokenLookup;
    }

    @Override
    protected Query newFieldQuery(Analyzer analyzer, String field, String queryText, boolean quoted) throws ParseException {
        return saveQueryLookupTokens(super.newFieldQuery(analyzer, field, queryText, quoted));
    }

    @Override
    protected Query newPrefixQuery(Term prefix) {
        return saveQueryLookupTokens(super.newPrefixQuery(prefix));
    }

    @Override
    protected Query newRegexpQuery(Term regexp) {
        return saveQueryLookupTokens(super.newRegexpQuery(regexp));
    }

    @Override
    protected Query newFuzzyQuery(Term term, float minimumSimilarity, int prefixLength) {
        return saveQueryLookupTokens(super.newFuzzyQuery(term, minimumSimilarity, prefixLength));
    }

    @Override
    protected Query newMatchAllDocsQuery() {
        return saveQueryLookupTokens(super.newMatchAllDocsQuery());
    }

    @Override
    protected Query newWildcardQuery(Term t) {
        return saveQueryLookupTokens(super.newWildcardQuery(t));
    }

    @Override
    protected Query newSynonymQuery(TermAndBoost[] terms) {
        return saveQueryLookupTokens(super.newSynonymQuery(terms));
    }

    @Override
    protected Query newGraphSynonymQuery(Iterator<Query> queries) {
        return saveQueryLookupTokens(super.newGraphSynonymQuery(queries));
    }

    @Override
    protected Query newTermQuery(Term term, float boost) {
        return saveQueryLookupTokens(super.newTermQuery(term, boost));
    }

    @Override
    protected Query newRangeQuery(String field, String part1, String part2, boolean startInclusive, boolean endInclusive) {
        return saveQueryLookupTokens(super.newRangeQuery(field, part1, part2, startInclusive, endInclusive));
    }

    @Override
    protected Query getBooleanQuery(List<BooleanClause> clauses) throws ParseException {
        final Query delegate = super.getBooleanQuery(clauses);
        return new FixedBooleanQuery((BooleanQuery) delegate);
    }

    /**
     * This method persists all newly discovered tokens that may be referenced in the query to a lookup for
     * later processing. This is the only place that binds query and its tokens together.
     *
     * This would be also good place to collect all subqueries if needed for any feature.
     */
    private Query saveQueryLookupTokens(Query query) {
        final Collection<ImmutableToken> tokens = CollectionUtils.subtract(tokenManager.getTokens(), processedTokens);
        if (!tokenLookup.containsKey(query) && !tokens.isEmpty()) {
            processedTokens.addAll(tokens);
            tokenLookup.put(query, tokens);
        }
        return query;
    }
}

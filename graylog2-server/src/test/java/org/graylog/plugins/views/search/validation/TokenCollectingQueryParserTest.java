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

import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParserConstants;
import org.apache.lucene.queryparser.classic.Token;
import org.apache.lucene.search.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.graylog.plugins.views.search.validation.LuceneQueryParser.ANALYZER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class TokenCollectingQueryParserTest {

    private TokenCollectingQueryParser toTest;

    private CollectingQueryParserTokenManager tokenManagerMock;
    private List<ImmutableToken> collectedTokensSimulation;
    private Term testTerm;

    @BeforeEach
    void setUp() {
        tokenManagerMock = mock(CollectingQueryParserTokenManager.class);
        collectedTokensSimulation = new ArrayList<>();
        doReturn(collectedTokensSimulation).when(tokenManagerMock).getTokens();
        toTest = new TokenCollectingQueryParser(tokenManagerMock, ParsedTerm.DEFAULT_FIELD, ANALYZER);

        testTerm = new Term("x", "nvmd");
    }

    @Test
    void testStoresNothingForQueryIfNoNewTokensCollected() {
        collectedTokensSimulation.clear();
        final Query query = toTest.newFuzzyQuery(testTerm, 1.0f, 2);
        final Map<Query, Collection<ImmutableToken>> tokenLookup = toTest.getTokenLookup();
        assertNull(tokenLookup.get(query));
    }

    @Test
    void testStoresTokensForQuery() {
        final ImmutableToken testToken = ImmutableToken.create(Token.newToken(QueryParserConstants.TERM, "blah"));
        collectedTokensSimulation.add(testToken);

        final Query query = toTest.newPrefixQuery(testTerm);

        final Map<Query, Collection<ImmutableToken>> tokenLookup = toTest.getTokenLookup();
        final Collection<ImmutableToken> queryTokens = tokenLookup.get(query);
        assertEquals(1, queryTokens.size());
        assertTrue(queryTokens.contains(testToken));
    }

    @Test
    void testStoresTokensForMultipleQueries() {
        final ImmutableToken testToken1 = ImmutableToken.create(Token.newToken(QueryParserConstants.TERM, "prefixToken"));
        collectedTokensSimulation.add(testToken1);
        final Query query1 = toTest.newPrefixQuery(testTerm);
        final ImmutableToken testToken2 = ImmutableToken.create(Token.newToken(QueryParserConstants.TERM, "regexpToken"));
        collectedTokensSimulation.add(testToken2);
        final Query query2 = toTest.newRegexpQuery(testTerm);
        final ImmutableToken testToken3_1 = ImmutableToken.create(Token.newToken(QueryParserConstants.TERM, "termToken1"));
        final ImmutableToken testToken3_2 = ImmutableToken.create(Token.newToken(QueryParserConstants.TERM, "termToken2"));
        collectedTokensSimulation.add(testToken3_1);
        collectedTokensSimulation.add(testToken3_2);
        final Query query3 = toTest.newTermQuery(testTerm, 1.0f);

        final Map<Query, Collection<ImmutableToken>> tokenLookup = toTest.getTokenLookup();
        final Collection<ImmutableToken> query1Tokens = tokenLookup.get(query1);
        assertEquals(1, query1Tokens.size());
        assertTrue(query1Tokens.contains(testToken1));
        final Collection<ImmutableToken> query2Tokens = tokenLookup.get(query2);
        assertEquals(1, query2Tokens.size());
        assertTrue(query2Tokens.contains(testToken2));
        final Collection<ImmutableToken> query3Tokens = tokenLookup.get(query3);
        assertEquals(2, query3Tokens.size());
        assertTrue(query3Tokens.contains(testToken3_1));
        assertTrue(query3Tokens.contains(testToken3_2));
    }

    @Test
    void testSeparatesTokensForTwoIdenticalQueries() {
        final ImmutableToken testToken1 = ImmutableToken.create(Token.newToken(QueryParserConstants.TERM, "blah1"));
        collectedTokensSimulation.add(testToken1);
        final Query query1 = toTest.newPrefixQuery(testTerm);
        final ImmutableToken testToken2 = ImmutableToken.create(Token.newToken(QueryParserConstants.TERM, "blah2"));
        collectedTokensSimulation.add(testToken2);
        final Query query2 = toTest.newPrefixQuery(testTerm);

        final Map<Query, Collection<ImmutableToken>> tokenLookup = toTest.getTokenLookup();
        final Collection<ImmutableToken> query1Tokens = tokenLookup.get(query1);
        assertEquals(1, query1Tokens.size());
        assertTrue(query1Tokens.contains(testToken1));
        final Collection<ImmutableToken> query2Tokens = tokenLookup.get(query2);
        assertEquals(1, query2Tokens.size());
        assertTrue(query2Tokens.contains(testToken2));
    }
}

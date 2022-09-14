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

import org.apache.lucene.queryparser.charstream.CharStream;
import org.apache.lucene.queryparser.charstream.FastCharStream;
import org.apache.lucene.queryparser.classic.QueryParserTokenManager;
import org.apache.lucene.queryparser.classic.Token;

import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;

public class CollectingQueryParserTokenManager extends QueryParserTokenManager {

    private final List<ImmutableToken> tokens = new LinkedList<>();

    public CollectingQueryParserTokenManager() {
        super(new FastCharStream(new StringReader("")));
    }

    @Override
    public void ReInit(CharStream stream) {
        this.tokens.clear();
        super.ReInit(new LineCountingCharStream(stream));
    }

    @Override
    public Token getNextToken() {
        final Token token = super.getNextToken();
        this.tokens.add(ImmutableToken.create(token));
        return token;
    }

    public List<ImmutableToken> getTokens() {
        return tokens;
    }
}

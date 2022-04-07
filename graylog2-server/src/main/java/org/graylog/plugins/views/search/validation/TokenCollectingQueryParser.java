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
import org.apache.lucene.queryparser.classic.QueryParser;

import java.util.List;

public class TokenCollectingQueryParser extends QueryParser {

    public TokenCollectingQueryParser(String defaultFieldName, Analyzer analyzer) {
        super(new CollectingQueryParserTokenManager(defaultFieldName, analyzer));
        init(defaultFieldName, analyzer);
    }

    public List<ImmutableToken> getTokens() {
        return ((CollectingQueryParserTokenManager) super.token_source).getTokens();
    }
}

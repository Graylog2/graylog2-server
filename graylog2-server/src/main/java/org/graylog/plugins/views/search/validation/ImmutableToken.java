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

import com.google.auto.value.AutoValue;
import org.apache.lucene.queryparser.classic.QueryParserConstants;

@AutoValue
public abstract class ImmutableToken {
    public abstract int kind();

    /**
     * The line number of the first character of this Token.
     */
    public abstract int beginLine();

    /**
     * The column number of the first character of this Token.
     */
    public abstract int beginColumn();

    /**
     * The line number of the last character of this Token.
     */
    public abstract int endLine();

    /**
     * The column number of the last character of this Token.
     */
    public abstract int endColumn();

    /**
     * The string image of the token.
     */
    public abstract String image();

    public static ImmutableToken create(org.apache.lucene.queryparser.classic.Token mutableToken) {
        return new AutoValue_ImmutableToken(mutableToken.kind, mutableToken.beginLine, mutableToken.beginColumn, mutableToken.endLine, mutableToken.endColumn, mutableToken.image);
    }

    public boolean matches(int tokenType, String tokenValue) {
        return kind() == tokenType && image().equals(tokenValue);
    }


    public boolean isInvalidOperator() {
        return kind() == QueryParserConstants.TERM && ("and".equals(image()) || "or".equals(image()) || "not".equals(image()));
    }
}

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
package org.graylog.plugins.pipelineprocessor.parser.errors;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RecognitionException;

import javax.annotation.Nullable;

public class SyntaxError extends ParseError {

    private final Object offendingSymbol;
    private final int line;
    private final int charPositionInLine;
    private final String msg;
    private final RecognitionException e;

    public SyntaxError(@Nullable Object offendingSymbol, int line, int charPositionInLine, String msg, @Nullable RecognitionException e) {
        super("syntax_error", new ParserRuleContext());

        this.offendingSymbol = offendingSymbol;
        this.line = line;
        this.charPositionInLine = charPositionInLine;
        this.msg = msg;
        this.e = e;
    }

    @Override
    public int line() {
        return line;
    }

    @Override
    public int positionInLine() {
        return charPositionInLine;
    }

    @JsonProperty("reason")
    @Override
    public String toString() {
        return msg;
    }
}

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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.Objects;

public abstract class ParseError {

    @JsonProperty
    private final String type;

    @JsonIgnore
    private final ParserRuleContext ctx;

    protected ParseError(String type, ParserRuleContext ctx) {
        this.type = type;
        this.ctx = ctx;
    }

    @JsonProperty
    public int line() {
        return ctx.getStart().getLine();
    }

    @JsonProperty
    public int positionInLine() {
        return ctx.getStart().getCharPositionInLine();
    }

    protected String positionString() {
        return " in" +
                " line " + line() +
                " pos " + positionInLine();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ParseError)) return false;
        ParseError that = (ParseError) o;
        return Objects.equals(type, that.type) &&
                Objects.equals(ctx, that.ctx);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, ctx);
    }
}

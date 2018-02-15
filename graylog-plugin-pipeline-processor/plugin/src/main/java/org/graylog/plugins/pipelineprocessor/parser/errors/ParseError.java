/**
 * This file is part of Graylog Pipeline Processor.
 *
 * Graylog Pipeline Processor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog Pipeline Processor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog Pipeline Processor.  If not, see <http://www.gnu.org/licenses/>.
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

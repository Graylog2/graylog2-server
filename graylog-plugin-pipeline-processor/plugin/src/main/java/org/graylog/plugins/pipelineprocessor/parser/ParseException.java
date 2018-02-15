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
package org.graylog.plugins.pipelineprocessor.parser;

import org.graylog.plugins.pipelineprocessor.parser.errors.ParseError;

import java.util.Set;

public class ParseException extends RuntimeException {
    private final Set<ParseError> errors;

    public ParseException(Set<ParseError> errors) {
        this.errors = errors;
    }

    public Set<ParseError> getErrors() {
        return errors;
    }

    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder("Errors:\n");
        for (ParseError parseError : getErrors()) {
            sb.append(" ").append(parseError).append("\n");
        }
        return sb.toString();
    }
}

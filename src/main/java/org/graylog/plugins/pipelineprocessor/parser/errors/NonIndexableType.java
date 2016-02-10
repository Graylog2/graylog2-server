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

import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog.plugins.pipelineprocessor.parser.RuleLangParser;

public class NonIndexableType extends ParseError {
    private final Class<?> indexableType;

    public NonIndexableType(RuleLangParser.IndexedAccessContext ctx, Class<?> indexableType) {
        super("non_indexable", ctx);
        this.indexableType = indexableType;
    }

    @JsonProperty("reason")
    @Override
    public String toString() {
        return "Cannot index value of type " + indexableType.getSimpleName() + positionString();
    }
}

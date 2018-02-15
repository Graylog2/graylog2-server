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
import org.graylog.plugins.pipelineprocessor.ast.functions.Function;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.graylog.plugins.pipelineprocessor.parser.RuleLangParser;

public class MissingRequiredParam extends ParseError {
    private final Function<?> function;
    private final ParameterDescriptor param;

    public MissingRequiredParam(RuleLangParser.FunctionCallContext ctx,
                                Function<?> function,
                                ParameterDescriptor param) {
        super("missing_required_param", ctx);
        this.function = function;
        this.param = param;
    }

    @JsonProperty("reason")
    @Override
    public String toString() {
        return "Missing required parameter " + param.name() +
                " of type " + param.type().getSimpleName() +
                " in call to function " + function.descriptor().name()
                + positionString();
    }
}

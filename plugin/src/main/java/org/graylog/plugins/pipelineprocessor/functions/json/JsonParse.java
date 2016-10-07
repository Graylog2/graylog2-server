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
package org.graylog.plugins.pipelineprocessor.functions.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

import java.io.IOException;

import static com.google.common.collect.ImmutableList.of;

public class JsonParse extends AbstractFunction<JsonNode> {
    private static final Logger log = LoggerFactory.getLogger(JsonParse.class);
    public static final String NAME = "parse_json";

    private final ObjectMapper objectMapper;
    private final ParameterDescriptor<String, String> valueParam;

    @Inject
    public JsonParse(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        valueParam = ParameterDescriptor.string("value").description("The string to parse as a JSON tree").build();
    }

    @Override
    public JsonNode evaluate(FunctionArgs args, EvaluationContext context) {
        final String value = valueParam.required(args, context);
        try {
            return objectMapper.readTree(value);
        } catch (IOException e) {
            log.warn("Unable to parse json", e);
        }
        return null;
    }

    @Override
    public FunctionDescriptor<JsonNode> descriptor() {
        return FunctionDescriptor.<JsonNode>builder()
                .name(NAME)
                .returnType(JsonNode.class)
                .params(of(
                        valueParam
                ))
                .description("Parses a string as a JSON tree")
                .build();
    }
}

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

    @Inject
    public JsonParse(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public JsonNode evaluate(FunctionArgs args, EvaluationContext context) {
        final String value = args.param("value").evalRequired(args, context, String.class);
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
                        ParameterDescriptor.string("value").build()
                ))
                .build();
    }
}

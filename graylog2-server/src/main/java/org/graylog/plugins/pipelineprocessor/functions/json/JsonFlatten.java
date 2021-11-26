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

public class JsonFlatten extends AbstractFunction<JsonNode> {
    private static final Logger LOG = LoggerFactory.getLogger(JsonFlatten.class);
    public static final String NAME = "flatten_json";
    private static final String OPTION_JSON = "json";
    private static final String OPTION_FLATTEN = "flatten";
    private static final String OPTION_IGNORE = "ignore";

    private final ObjectMapper objectMapper;
    private final ParameterDescriptor<String, String> valueParam;
    private final ParameterDescriptor<String, String> arrayHandlerParam;

    @Inject
    public JsonFlatten(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        valueParam = ParameterDescriptor.string("value").description("The string to parse as a JSON tree").build();
        arrayHandlerParam = ParameterDescriptor.string("array_handler").description("Determines how arrays are processed").build();
    }

    @Override
    public JsonNode evaluate(FunctionArgs args, EvaluationContext context) {
        final String value = valueParam.required(args, context);
        final String arrayHandler = arrayHandlerParam.required(args, context);

        try {
            switch (arrayHandler) {
                case OPTION_IGNORE:
                    return JsonUtils.extractJson(value, objectMapper, true, false, true);
                case OPTION_JSON:
                    return JsonUtils.extractJson(value, objectMapper, true, true, false);
                case OPTION_FLATTEN:
                    return JsonUtils.extractJson(value, objectMapper, true, false, false);
                default:
                    LOG.warn("Unknown parameter array_handler: {}", arrayHandler);
            }
        } catch (IOException e) {
            LOG.warn("Unable to parse JSON", e);
        }
        return null;
    }

    @Override
    public FunctionDescriptor<JsonNode> descriptor() {
        return FunctionDescriptor.<JsonNode>builder()
                .name(NAME)
                .returnType(JsonNode.class)
                .params(of(
                        valueParam, arrayHandlerParam
                ))
                .description("Parses a string as a JSON tree, while flattening all containers to a single level")
                .build();
    }

}

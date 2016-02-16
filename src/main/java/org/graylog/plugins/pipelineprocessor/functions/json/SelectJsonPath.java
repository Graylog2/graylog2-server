package org.graylog.plugins.pipelineprocessor.functions.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.TypeLiteral;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.expressions.Expression;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.collect.ImmutableList.of;

public class SelectJsonPath extends AbstractFunction<Map<String, Object>> {

    public static final String NAME = "select_jsonpath";
    private final Configuration configuration;

    @Inject
    public SelectJsonPath(ObjectMapper objectMapper) {
        configuration = Configuration.builder()
                .options(Option.SUPPRESS_EXCEPTIONS)
                .jsonProvider(new JacksonJsonNodeJsonProvider(objectMapper))
                .build();
    }

    @Override
    public Object preComputeConstantArgument(FunctionArgs args, String name, Expression arg) {
        if ("paths".equals(name)) {
            final Object o = super.preComputeConstantArgument(args, name, arg);
            //noinspection unchecked
            final HashMap<String, String> map = (HashMap<String, String>) o;
            return map.entrySet().stream().collect(Collectors.toMap(
                    Map.Entry::getKey,
                    stringStringEntry -> JsonPath.compile(stringStringEntry.getValue())
            ));
        }
        return super.preComputeConstantArgument(args, name, arg);
    }

    @Override
    public Map<String, Object> evaluate(FunctionArgs args, EvaluationContext context) {
        final JsonNode json = args.param("json").evalRequired(args, context, JsonNode.class);
        //noinspection unchecked
        final Map<String, JsonPath> paths = args.param("paths").evalRequired(args, context, Map.class);

        return paths.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().read(json, configuration)
                ));
    }

    @Override
    public FunctionDescriptor<Map<String, Object>> descriptor() {
        //noinspection unchecked
        return FunctionDescriptor.<Map<String, Object>>builder()
                .name(NAME)
                .returnType((Class<? extends Map<String, Object>>) new TypeLiteral<Map<String,Object>>() {}.getRawType())
                .params(of(
                        ParameterDescriptor.type("json", JsonNode.class).build(),
                        ParameterDescriptor
                                .type("paths", Map.class, Map.class)
                                .build()
                ))
                .build();
    }

}

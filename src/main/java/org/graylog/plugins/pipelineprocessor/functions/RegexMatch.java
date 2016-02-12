package org.graylog.plugins.pipelineprocessor.functions;

import com.google.common.collect.ForwardingMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.expressions.Expression;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.collect.ImmutableList.of;

public class RegexMatch extends AbstractFunction<RegexMatch.RegexMatchResult> {

    public static final String PATTERN_ARG = "pattern";
    public static final String VALUE_ARG = "value";
    public static final String GROUP_NAMES_ARG = "group_names";
    public static final String NAME = "regex";

    @Override
    public Object preComputeConstantArgument(FunctionArgs args, String name, Expression arg) {
        final String stringValue = (String) super.preComputeConstantArgument(args, name, arg);
        switch (name) {
            case PATTERN_ARG:
                return Pattern.compile(stringValue);
        }
        return stringValue;
    }

    @Override
    public RegexMatchResult evaluate(FunctionArgs args, EvaluationContext context) {
        final Pattern regex = args.required(PATTERN_ARG, context, Pattern.class);
        final String value = args.required(VALUE_ARG, context, String.class);
        if (regex == null || value == null) {
            throw new IllegalArgumentException();
        }
        //noinspection unchecked
        final List<String> groupNames =
                (List<String>) args.evaluated(GROUP_NAMES_ARG, context, List.class)
                        .orElse(Collections.emptyList());

        final Matcher matcher = regex.matcher(value);
        final boolean matches = matcher.matches();

        return new RegexMatchResult(matches, matcher.toMatchResult(), groupNames);

    }

    @Override
    public FunctionDescriptor<RegexMatchResult> descriptor() {
        return FunctionDescriptor.<RegexMatchResult>builder()
                .name(NAME)
                .pure(true)
                .returnType(RegexMatchResult.class)
                .params(of(
                        ParameterDescriptor.string(PATTERN_ARG, Pattern.class).transform(Pattern::compile).build(),
                        ParameterDescriptor.string(VALUE_ARG).build(),
                        ParameterDescriptor.type(GROUP_NAMES_ARG, List.class).optional().build()
                ))
                .build();
    }

    /**
     * The bean returned into the rule engine. It implements Map so rules can access it directly.
     * <br/>
     * At the same time there's an additional <code>matches</code> bean property to quickly check whether the regex has matched at all.
     */
    public static class RegexMatchResult extends ForwardingMap<String, String> {
        private final boolean matches;
        private final ImmutableMap<String, String> groups;

        public RegexMatchResult(boolean matches, MatchResult matchResult, List<String> groupNames) {
            this.matches = matches;
            ImmutableMap.Builder<String, String> builder = ImmutableMap.<String, String>builder();

            if (matches) {
                // arggggh! not 0 based.
                final int groupCount = matchResult.groupCount();
                for (int i = 1; i <= groupCount; i++) {
                    final String groupValue = matchResult.group(i);
                    // try to get a group name, if that fails use a 0-based index as the name
                    final String groupName = Iterables.get(groupNames, i - 1, null);
                    builder.put(groupName != null ? groupName : String.valueOf(i - 1), groupValue);
                }
            }
            groups = builder.build();
        }

        public boolean isMatches() {
            return matches;
        }

        public Map<String, String> getGroups() {
            return groups;
        }

        @Override
        protected Map<String, String> delegate() {
            return getGroups();
        }
    }
}

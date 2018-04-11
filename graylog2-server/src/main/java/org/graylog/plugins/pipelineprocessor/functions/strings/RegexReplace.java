/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.plugins.pipelineprocessor.functions.strings;

import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;

import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.ImmutableList.of;

public class RegexReplace extends AbstractFunction<String> {

    public static final String NAME = "regex_replace";
    private final ParameterDescriptor<String, Pattern> patternParam;
    private final ParameterDescriptor<String, String> valueParam;
    private final ParameterDescriptor<String, String> replacementParam;
    private final ParameterDescriptor<Boolean, Boolean> replaceAllParam;

    public RegexReplace() {
        patternParam = ParameterDescriptor.string("pattern", Pattern.class).transform(Pattern::compile).description("The regular expression to which the \"value\" string is to be matched; uses Java regex syntax").build();
        valueParam = ParameterDescriptor.string("value").description("The string to match the pattern against").build();
        replacementParam = ParameterDescriptor.string("replacement").description("The string to be substituted for the first or all matches").build();
        replaceAllParam = ParameterDescriptor.bool("replace_all").optional().description("Replace all matches if \"true\", otherwise only replace the first match. Default: true").build();
    }

    @Override
    public String evaluate(FunctionArgs args, EvaluationContext context) {
        final Pattern pattern = patternParam.required(args, context);
        final String value = valueParam.required(args, context);
        final String replacement = replacementParam.required(args, context);
        final boolean replaceAll = replaceAllParam.optional(args, context).orElse(true);

        checkArgument(pattern != null, "Argument 'pattern' cannot be 'null'");
        checkArgument(value != null, "Argument 'value' cannot be 'null'");
        checkArgument(replacement != null, "Argument 'replacement' cannot be 'null'");

        if (replaceAll) {
            return pattern.matcher(value).replaceAll(replacement);
        } else {
            return pattern.matcher(value).replaceFirst(replacement);
        }
    }

    @Override
    public FunctionDescriptor<String> descriptor() {
        return FunctionDescriptor.<String>builder()
                .name(NAME)
                .pure(true)
                .returnType(String.class)
                .params(of(patternParam, valueParam, replacementParam, replaceAllParam))
                .description("Match a string with a regular expression (Java syntax)")
                .build();
    }
}

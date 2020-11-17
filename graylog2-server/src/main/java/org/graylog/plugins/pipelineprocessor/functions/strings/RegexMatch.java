/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.plugins.pipelineprocessor.functions.strings;

import com.google.common.collect.ForwardingMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
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

    public static final String NAME = "regex";
    private final ParameterDescriptor<String, Pattern> pattern;
    private final ParameterDescriptor<String, String> value;
    private final ParameterDescriptor<List, List> optionalGroupNames;

    public RegexMatch() {
        pattern = ParameterDescriptor.string("pattern", Pattern.class).transform(Pattern::compile).description("The regular expression to match against 'value', uses Java regex syntax").build();
        value = ParameterDescriptor.string("value").description("The string to match the pattern against").build();
        optionalGroupNames = ParameterDescriptor.type("group_names", List.class).optional().description("List of names to use for matcher groups").build();
    }

    @Override
    public RegexMatchResult evaluate(FunctionArgs args, EvaluationContext context) {
        final Pattern regex = pattern.required(args, context);
        final String value = this.value.required(args, context);
        if (regex == null || value == null) {
            final String nullArgument = regex == null ? "pattern" : "value";
            throw new IllegalArgumentException("Argument '" + nullArgument + "' cannot be 'null'");
        }
        //noinspection unchecked
        final List<String> groupNames =
                (List<String>) optionalGroupNames.optional(args, context).orElse(Collections.emptyList());

        final Matcher matcher = regex.matcher(value);
        final boolean matches = matcher.find();

        return new RegexMatchResult(matches, matcher.toMatchResult(), groupNames);

    }

    @Override
    public FunctionDescriptor<RegexMatchResult> descriptor() {
        return FunctionDescriptor.<RegexMatchResult>builder()
                .name(NAME)
                .pure(true)
                .returnType(RegexMatchResult.class)
                .params(of(
                        pattern,
                        value,
                        optionalGroupNames
                ))
                .description("Match a string with a regular expression (Java syntax)")
                .build();
    }

    /**
     * The bean returned into the rule engine. It implements Map so rules can access it directly.
     * <br>
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

                    if (groupValue == null) {
                        // You cannot add null values to an ImmutableMap but optional matcher groups may be null.
                        continue;
                    }

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

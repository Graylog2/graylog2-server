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
package org.graylog.plugins.pipelineprocessor.functions.strings;

import com.google.common.collect.ForwardingMap;
import oi.thekraken.grok.api.Grok;
import oi.thekraken.grok.api.Match;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.graylog2.grok.GrokPatternRegistry;

import javax.inject.Inject;
import java.util.Map;

import static com.google.common.collect.ImmutableList.of;

public class GrokMatch extends AbstractFunction<GrokMatch.GrokResult> {

    public static final String NAME = "grok";

    private final ParameterDescriptor<String, String> valueParam;
    private final ParameterDescriptor<String, String> patternParam;
    private final ParameterDescriptor<Boolean, Boolean> namedOnly;

    private final GrokPatternRegistry grokPatternRegistry;

    @Inject
    public GrokMatch(GrokPatternRegistry grokPatternRegistry) {
        this.grokPatternRegistry = grokPatternRegistry;

        valueParam = ParameterDescriptor.string("value").description("The string to apply the Grok pattern against").build();
        patternParam = ParameterDescriptor.string("pattern").description("The Grok pattern").build();
        namedOnly = ParameterDescriptor.bool("only_named_captures").optional().description("Whether to only use explicitly named groups in the patterns").build();
    }

    @Override
    public GrokResult evaluate(FunctionArgs args, EvaluationContext context) {
        final String value = valueParam.required(args, context);
        final String pattern = patternParam.required(args, context);
        final boolean onlyNamedCaptures = namedOnly.optional(args, context).orElse(false);

        if (value == null || pattern == null) {
            return null;
        }

        final Grok grok = grokPatternRegistry.cachedGrokForPattern(pattern, onlyNamedCaptures);

        final Match match = grok.match(value);
        match.captures();
        return new GrokResult(match.toMap());
    }

    @Override
    public FunctionDescriptor<GrokResult> descriptor() {
        return FunctionDescriptor.<GrokResult>builder()
                .name(NAME)
                .returnType(GrokResult.class)
                .params(of(patternParam, valueParam, namedOnly))
                .description("Applies a Grok pattern to a string")
                .build();
    }

    public static class GrokResult extends ForwardingMap<String, Object> {
        private final Map<String, Object> captures;

        public GrokResult(Map<String, Object> captures) {
            this.captures = captures;
        }

        @Override
        protected Map<String, Object> delegate() {
            return captures;
        }

        public boolean isMatches() {
            return captures.size() > 0;
        }
    }


}

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
package org.graylog.plugins.pipelineprocessor.functions;

import io.krakens.grok.api.Grok;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.graylog2.grok.GrokPatternRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.collect.ImmutableList.of;

import javax.inject.Inject;

public class GrokExists extends AbstractFunction<Boolean> {

    private static final Logger log = LoggerFactory.getLogger(GrokExists.class);
    public static final String NAME = "grok_exists";

    private final ParameterDescriptor<String, String> patternParam;
    private final ParameterDescriptor<Boolean, Boolean> doLog;

    private final GrokPatternRegistry grokPatternRegistry;

    @Inject
    public GrokExists(GrokPatternRegistry grokPatternRegistry) {
        this.grokPatternRegistry = grokPatternRegistry;

        patternParam = ParameterDescriptor.string("pattern")
                .description("The Grok Pattern which is to be tested for existance.").build();
        doLog = ParameterDescriptor.bool("do_log").optional()
                .description("Log if the Grok Pattern is missing.").build();
    }

    @Override
    public Boolean evaluate(FunctionArgs args, EvaluationContext context) {
        final String pattern = patternParam.required(args, context);
        final boolean logWhenNotFound = doLog.optional(args, context).orElse(false);

        if (pattern == null) {
            return null;
        }

        final boolean patternExists = grokPatternRegistry.grokPatternExists(pattern);
        if (!patternExists && logWhenNotFound) {
           log.info("Grok Pattern " + pattern + " was not found in pipeline rule function execution");
        }

        return patternExists;
    }

    @Override
    public FunctionDescriptor<Boolean> descriptor() {
       return FunctionDescriptor.<Boolean>builder()
               .name(NAME)
               .returnType(Boolean.class)
               .params(of(patternParam, doLog))
               .description("Tests if the searched Grok Pattern exists.")
               .build();
    }
}

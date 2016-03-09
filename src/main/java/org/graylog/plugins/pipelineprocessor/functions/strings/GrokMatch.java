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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ForwardingMap;
import oi.thekraken.grok.api.Grok;
import oi.thekraken.grok.api.Match;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.graylog2.grok.GrokPattern;
import org.graylog2.grok.GrokPatternService;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.cache.CacheLoader.asyncReloading;
import static com.google.common.collect.ImmutableList.of;

public class GrokMatch extends AbstractFunction<GrokMatch.GrokResult> {

    public static final String NAME = "grok";
    private final ParameterDescriptor<String, String> valueParam;
    private final ParameterDescriptor<String, String> patternParam;
    private final LoadingCache<String, Grok> grokCache;

    @Inject
    public GrokMatch(GrokPatternService grokPatternService, @Named("daemonScheduler") ScheduledExecutorService daemonExecutor) {
        valueParam = ParameterDescriptor.string("value").build();
        patternParam = ParameterDescriptor.string("pattern").build();

        grokCache = CacheBuilder.newBuilder()
                .expireAfterAccess(1, TimeUnit.MINUTES)
                .build(asyncReloading(new GrokReloader(grokPatternService), daemonExecutor));
    }

    @Override
    public GrokResult evaluate(FunctionArgs args, EvaluationContext context) {
        final String value = valueParam.required(args, context);
        final String pattern = patternParam.required(args, context);
        if (value == null || pattern == null) {
            return null;
        }

        final Grok grok;
        try {
            grok = grokCache.get(pattern);
        } catch (ExecutionException e) {
            // this can happen when the pattern is malformed
            throw new RuntimeException(e);
        }
        final Match match = grok.match(value);
        match.captures();
        return new GrokResult(match.toMap());
    }

    @Override
    public FunctionDescriptor<GrokResult> descriptor() {
        return FunctionDescriptor.<GrokResult>builder()
                .name(NAME)
                .returnType(GrokResult.class)
                .params(of(patternParam, valueParam))
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

    private static class GrokReloader extends CacheLoader<String, Grok> {
        private final GrokPatternService grokPatternService;

        public GrokReloader(GrokPatternService grokPatternService) {
            this.grokPatternService = grokPatternService;
        }

        @Override
        public Grok load(@Nonnull String pattern) throws Exception {
            final Grok grok = new Grok();
            for (GrokPattern grokPattern : grokPatternService.loadAll()) {
                if (!isNullOrEmpty(grokPattern.name) || isNullOrEmpty(grokPattern.pattern)) {
                    grok.addPattern(grokPattern.name, grokPattern.pattern);
                }
            }
            grok.compile(pattern);
            return grok;
        }
    }
}

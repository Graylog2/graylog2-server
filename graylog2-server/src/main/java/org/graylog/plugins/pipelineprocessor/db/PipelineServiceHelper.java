package org.graylog.plugins.pipelineprocessor.db;

import com.google.common.collect.ImmutableList;
import org.graylog.plugins.pipelineprocessor.ast.Pipeline;
import org.graylog.plugins.pipelineprocessor.parser.PipelineRuleParser;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.constraints.NotNull;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
public class PipelineServiceHelper {

    private final PipelineRuleParser pipelineParser;

    @Inject
    public PipelineServiceHelper(PipelineRuleParser pipelineRuleParser) {
        this.pipelineParser = pipelineRuleParser;
    }

    @NotNull
    public List<PipelineDao> filterByRuleName(@NotNull Supplier<Collection<PipelineDao>> pipelines, @NotNull Set<String> ruleNames) {
        if (ruleNames.isEmpty()) {
            return ImmutableList.of();
        }

        return pipelines.get().stream()
                .map(pipelineDao -> new ParsedPipeline(pipelineDao, pipelineParser.parsePipeline(pipelineDao.id(), pipelineDao.source())))
                .filter(wrapper -> wrapper.parsed
                        .stages()
                        .stream()
                        .flatMap(stage -> stage.ruleReferences() == null ? Stream.empty() : stage.ruleReferences().stream())
                        .anyMatch(ruleNames::contains))
                .map(wrapper -> wrapper.source)
                .collect(Collectors.toList());
    }

    private static class ParsedPipeline {
        private final PipelineDao source;
        private final Pipeline parsed;

        public ParsedPipeline(PipelineDao source, Pipeline parsed) {
            this.source = source;
            this.parsed = parsed;
        }
    }
}

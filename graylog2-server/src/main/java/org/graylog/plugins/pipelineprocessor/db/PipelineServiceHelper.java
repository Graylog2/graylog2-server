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
package org.graylog.plugins.pipelineprocessor.db;

import com.google.common.collect.ImmutableList;
import org.graylog.plugins.pipelineprocessor.ast.Pipeline;
import org.graylog.plugins.pipelineprocessor.parser.ParseException;
import org.graylog.plugins.pipelineprocessor.parser.PipelineRuleParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private final Logger logger = LoggerFactory.getLogger(getClass());

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
                .flatMap(pipelineDao -> {
                    try {
                        final Pipeline parsedPipeline = pipelineParser.parsePipeline(pipelineDao.id(), pipelineDao.source());
                        return Stream.of(new ParsedPipeline(pipelineDao, parsedPipeline));
                    } catch (ParseException e) {
                        logger.warn("Ignoring non-parseable pipeline <{}/{}> with errors <{}>", pipelineDao.title(), pipelineDao.id(), e.getErrors());
                        return Stream.empty();
                    }
                })
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

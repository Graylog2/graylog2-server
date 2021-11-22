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

import com.google.common.collect.ImmutableMap;
import org.graylog.plugins.pipelineprocessor.ast.Pipeline;
import org.graylog.plugins.pipelineprocessor.parser.ParseException;
import org.graylog.plugins.pipelineprocessor.parser.PipelineRuleParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

@Singleton
public class PipelineServiceHelper {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final PipelineRuleParser pipelineParser;

    @Inject
    public PipelineServiceHelper(PipelineRuleParser pipelineRuleParser) {
        this.pipelineParser = pipelineRuleParser;
    }

    @Nonnull
    public Map<String, List<PipelineDao>> groupByRuleName(@Nonnull Supplier<Collection<PipelineDao>> pipelines, @Nonnull Set<String> ruleNames) {
        if (ruleNames.isEmpty()) {
            return ImmutableMap.of();
        }

        final Map<String, List<PipelineDao>> result = new HashMap<>();

        pipelines.get().stream()
                .flatMap(pipelineDao -> {
                    try {
                        final Pipeline parsedPipeline = pipelineParser.parsePipeline(pipelineDao.id(), pipelineDao.source());
                        return Stream.of(new ParsedPipelineWithSource(pipelineDao, parsedPipeline));
                    } catch (ParseException e) {
                        logger.warn("Ignoring non-parseable pipeline <{}/{}> with errors <{}>", pipelineDao.title(), pipelineDao.id(), e.getErrors());
                        return Stream.empty();
                    }
                }).forEach(pp -> {
                    for (String ruleName : ruleNames) {
                        if (!result.containsKey(ruleName)) {
                            result.put(ruleName, new ArrayList<>());
                        }
                        if (pp.parsed.containsRule(ruleName)) {
                            result.get(ruleName).add(pp.source);
                        }
                    }
                });

        return ImmutableMap.copyOf(result);
    }

    static final class ParsedPipelineWithSource {
        private final PipelineDao source;
        private final Pipeline parsed;

        ParsedPipelineWithSource(@Nonnull PipelineDao source, @Nonnull Pipeline parsed) {
            this.source = source;
            this.parsed = parsed;
        }
    }
}

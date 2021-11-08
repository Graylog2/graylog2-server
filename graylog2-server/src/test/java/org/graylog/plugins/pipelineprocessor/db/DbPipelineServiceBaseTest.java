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
import org.apache.commons.lang3.StringUtils;
import org.graylog.plugins.pipelineprocessor.ast.functions.Function;
import org.graylog.plugins.pipelineprocessor.parser.FunctionRegistry;
import org.graylog.plugins.pipelineprocessor.parser.PipelineRuleParser;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;
import org.testcontainers.shaded.com.google.common.collect.ImmutableSet;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.shared.utilities.StringUtils.f;

public abstract class DbPipelineServiceBaseTest {


    private PipelineService underTest;

    @Before
    public void setup() {
        final Map<String, Function<?>> functions = ImmutableMap.of();
        final PipelineRuleParser pipelineRuleParser = new PipelineRuleParser(new FunctionRegistry(functions));
        final PipelineServiceHelper pipelineServiceHelper = new PipelineServiceHelper(pipelineRuleParser);

        underTest = createPipelineService(pipelineServiceHelper);
    }

    abstract protected PipelineService createPipelineService(PipelineServiceHelper pipelineServiceHelper);

    @Test
    public void loadByRules_returnsAllPipelinesUsingProvidedRules() {
        // given
        saveNewPipelineDao("Pipeline 1", pipelineSource("Pipeline 1",
                "rule \"debug#1\"\n" + "rule\"debug#2\"\n", ""));

        saveNewPipelineDao("Pipeline 2", pipelineSource("Pipeline 2", "", ""));

        saveNewPipelineDao("Pipeline 3", pipelineSource("Pipeline 3",
                "rule   \"debug#2\"\n",
                "rule  \"debug#3\"\n"));

        saveNewPipelineDao("Pipeline 4", pipelineSource("Pipeline 4",
                "rule     \"debug#3\"\n",
                ""));

        // when + then
        assertThat(underTest.loadByRules(ImmutableSet.of("debug#3"))).satisfies(containsPipelines("Pipeline 3", "Pipeline 4"));
        assertThat(underTest.loadByRules(ImmutableSet.of("debug#2", "debug#3"))).satisfies(containsPipelines("Pipeline 1", "Pipeline 3", "Pipeline 4"));
        assertThat(underTest.loadByRules(ImmutableSet.of())).satisfies(containsPipelines());
        assertThat(underTest.loadByRules(ImmutableSet.of("debug#4"))).satisfies(containsPipelines());
    }

    @Test
    public void performanceTest() {
        // given
        IntStream.range(0, 1000).forEach(i -> {
            saveNewPipelineDao("Pipeline " + i, pipelineSource(Integer.toString(i),
                    StringUtils.repeat(ruleReference("debug#1"), 50),
                    StringUtils.repeat(ruleReference("debug#2"), 50)));
        });

        final long started = System.currentTimeMillis();
        assertThat(underTest.loadByRules(ImmutableSet.of("debug#2", "debug#3"))).hasSize(1000);
        final long took = System.currentTimeMillis() - started;
        assertThat(took).isLessThan(1000);
    }

    private String pipelineSource(String name, String stage0Rules, String stage1Rules) {
        return f("pipeline \"Pipeline %s\"\n" +
                        "stage 0 match either\n" +
                        "%s" +
                        "stage 1 match either\n" +
                        "%s" +
                        "end",
                name,
                String.join("", stage0Rules),
                String.join("", stage1Rules)
        );
    }

    private String ruleReference(String ruleName) {
        return f("rule \"%s\"\n", ruleName);
    }

    private void saveNewPipelineDao(String title, String source) {
        underTest.save(PipelineDao.builder()
                .title(title)
                .description("Description")
                .createdAt(DateTime.now(DateTimeZone.UTC))
                .modifiedAt(DateTime.now(DateTimeZone.UTC))
                .source(source)
                .build());
    }

    private Consumer<List<? extends PipelineDao>> containsPipelines(String... pipelineTitles) {
        return pipelines -> {
            assertThat(pipelines).hasSize(pipelineTitles.length);

            for (String pt : pipelineTitles) {
                assertThat(pipelines).anySatisfy(p -> {
                    assertThat(p.title()).isEqualTo(pt);
                });
            }
        };
    }
}

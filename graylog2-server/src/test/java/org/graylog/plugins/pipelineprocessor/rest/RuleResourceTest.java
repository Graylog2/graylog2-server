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
package org.graylog.plugins.pipelineprocessor.rest;

import com.google.common.collect.ImmutableSet;
import org.graylog.plugins.pipelineprocessor.db.PaginatedRuleService;
import org.graylog.plugins.pipelineprocessor.db.PipelineDao;
import org.graylog.plugins.pipelineprocessor.db.PipelineService;
import org.graylog.plugins.pipelineprocessor.db.PipelineServiceHelper;
import org.graylog.plugins.pipelineprocessor.db.RuleDao;
import org.graylog.plugins.pipelineprocessor.db.RuleMetricsConfigService;
import org.graylog.plugins.pipelineprocessor.db.RuleService;
import org.graylog.plugins.pipelineprocessor.parser.FunctionRegistry;
import org.graylog.plugins.pipelineprocessor.parser.PipelineRuleParser;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.testcontainers.shaded.com.google.common.collect.ImmutableList;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.shared.utilities.StringUtils.f;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class RuleResourceTest {

    @Mock
    RuleService ruleService;

    @Mock
    PipelineService pipelineService;

    @Mock
    RuleMetricsConfigService ruleMetricsConfigService;

    @Mock
    PipelineRuleParser pipelineRuleParser;

    @Mock
    PaginatedRuleService paginatedRuleService;

    @Mock
    FunctionRegistry functionRegistry;

    @Mock
    PipelineServiceHelper pipelineServiceHelper;

    RuleResource underTest;

    @Before
    public void setup() {
        underTest = new RuleResource(ruleService, pipelineService, ruleMetricsConfigService,
                pipelineRuleParser, paginatedRuleService, functionRegistry, pipelineServiceHelper);
    }

    @Test
    public void prepareContextForPaginatedResponse_returnsEmptyMapOnEmptyListOfRules() {
        assertThat(underTest.prepareContextForPaginatedResponse(ImmutableList.of()))
                .isEqualTo(ImmutableMap.of("used_in_pipelines", ImmutableMap.of()));
    }

    @Test
    public void prepareContextForPaginatedResponse_returnsEmptyRuleMapIfRulesNotUsedByPipelines() {
        final List<RuleDao> rules = ImmutableList.of(
                ruleDao("rule-1", "Rule 1"),
                ruleDao("rule-2", "Rule 2")
        );

        assertThat(underTest.prepareContextForPaginatedResponse(rules))
                .isEqualTo(ImmutableMap.of("used_in_pipelines", ImmutableMap.of(
                        "rule-1", ImmutableList.of(),
                        "rule-2", ImmutableList.of()
                )));
    }

    @Test
    public void prepareContextForPaginatedResponse_returnsRuleUsageMapIfRulesUsedByPipelines() {
        final List<RuleDao> rules = ImmutableList.of(
                ruleDao("rule-1", "Rule 1"),
                ruleDao("rule-2", "Rule 2"),
                ruleDao("rule-3", "Rule 3"),
                ruleDao("rule-4", "Rule 4")
        );

        when(pipelineServiceHelper.groupByRuleName(any(), eq(ImmutableSet.of("Rule 1", "Rule 2", "Rule 3", "Rule 4"))))
                .thenReturn(ImmutableMap.of(
                        "Rule 1", ImmutableList.of(pipelineDao("pipeline-1", "Pipeline 1")),
                        "Rule 2", ImmutableList.of(pipelineDao("pipeline-2", "Pipeline 2")),
                        "Rule 3", ImmutableList.of(
                                pipelineDao("pipeline-1", "Pipeline 1"),
                                pipelineDao("pipeline-2", "Pipeline 2"),
                                pipelineDao("pipeline-3", "Pipeline 3")
                        ),
                        "Rule 4", ImmutableList.of()
                ));

        assertThat(underTest.prepareContextForPaginatedResponse(rules))
                .isEqualTo(ImmutableMap.of("used_in_pipelines", ImmutableMap.of(
                        "rule-1", ImmutableList.of(PipelineCompactSource.create("pipeline-1", "Pipeline 1")),
                        "rule-2", ImmutableList.of(PipelineCompactSource.create("pipeline-2", "Pipeline 2")),
                        "rule-3", ImmutableList.of(
                                PipelineCompactSource.create("pipeline-1", "Pipeline 1"),
                                PipelineCompactSource.create("pipeline-2", "Pipeline 2"),
                                PipelineCompactSource.create("pipeline-3", "Pipeline 3")
                        ),
                        "rule-4", ImmutableList.of()
                )));
    }

    public RuleDao ruleDao(String id, String title) {
        return RuleDao.create(id, title, null, f("rule \"%s\"\n" +
                "when true\n" +
                "then\n" +
                "   debug(\"OK\");\n" +
                "end", title), null, null);
    }

    private PipelineDao pipelineDao(String id, String title) {
        return PipelineDao.builder()
                .id(id)
                .title(title)
                .description("Description")
                .createdAt(DateTime.now(DateTimeZone.UTC))
                .modifiedAt(DateTime.now(DateTimeZone.UTC))
                .source("Source")
                .build();
    }
}

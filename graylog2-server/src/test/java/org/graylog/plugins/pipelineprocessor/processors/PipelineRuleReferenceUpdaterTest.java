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
package org.graylog.plugins.pipelineprocessor.processors;

import com.google.common.eventbus.EventBus;
import org.graylog.plugins.pipelineprocessor.db.PipelineDao;
import org.graylog.plugins.pipelineprocessor.db.PipelineService;
import org.graylog.plugins.pipelineprocessor.db.RuleService;
import org.graylog.plugins.pipelineprocessor.events.RulesChangedEvent;
import org.graylog.plugins.pipelineprocessor.parser.PipelineRuleParser;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class PipelineRuleReferenceUpdaterTest {

    @Mock
    private PipelineService pipelineService;

    @Mock
    private RuleService ruleService;

    @Mock
    private PipelineRuleParser pipelineRuleParser;

    @Mock
    private EventBus eventBus;

    private PipelineRuleReferenceUpdater updater;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        updater = new PipelineRuleReferenceUpdater(
                pipelineService,
                ruleService,
                pipelineRuleParser,
                eventBus
        );
    }

    @Test
    public void testHandleRuleRename() {
        // Prepare test data
        String oldRuleName = "old_rule";
        String newRuleName = "new_rule";
        String pipelineSource = "pipeline \"test\"\nstage 0 match either\nrule \"old_rule\"\nend";

        PipelineDao pipeline = PipelineDao.builder()
                .id("pipeline-1")
                .title("Test Pipeline")
                .source(pipelineSource)
                .build();

        when(pipelineService.loadAll()).thenReturn(Collections.singletonList(pipeline));

        // Create rename event
        RulesChangedEvent event = RulesChangedEvent.updatedRule("rule-1", newRuleName, oldRuleName);

        // Execute
        updater.handleRuleChanges(event);

        // Verify that pipeline service was called to save updated pipeline
        ArgumentCaptor<PipelineDao> pipelineCaptor = ArgumentCaptor.forClass(PipelineDao.class);
        verify(pipelineService).save(pipelineCaptor.capture(), eq(false));

        // Verify the source was updated
        PipelineDao updatedPipeline = pipelineCaptor.getValue();
        assertThat(updatedPipeline.source()).contains(newRuleName);
        assertThat(updatedPipeline.source()).doesNotContain(oldRuleName);
    }

    @Test
    public void testHandleRuleDeletion() {
        // Prepare test data
        String ruleName = "rule_to_delete";
        String pipelineSource = "pipeline \"test\"\nstage 0 match either\nrule \"rule_to_delete\"\nrule \"keep_this\"\nend";

        PipelineDao pipeline = PipelineDao.builder()
                .id("pipeline-1")
                .title("Test Pipeline")
                .source(pipelineSource)
                .build();

        when(pipelineService.loadAll()).thenReturn(Collections.singletonList(pipeline));

        // Create delete event
        RulesChangedEvent event = RulesChangedEvent.deletedRule("rule-1", ruleName);

        // Execute
        updater.handleRuleChanges(event);

        // Verify that pipeline service was called to save updated pipeline
        ArgumentCaptor<PipelineDao> pipelineCaptor = ArgumentCaptor.forClass(PipelineDao.class);
        verify(pipelineService).save(pipelineCaptor.capture(), eq(false));

        // Verify the rule was removed
        PipelineDao updatedPipeline = pipelineCaptor.getValue();
        assertThat(updatedPipeline.source()).doesNotContain(ruleName);
        assertThat(updatedPipeline.source()).contains("keep_this");
    }

    @Test
    public void testNoUpdateWhenRuleNotReferenced() {
        // Prepare test data - pipeline doesn't reference the rule
        String pipelineSource = "pipeline \"test\"\nstage 0 match either\nrule \"other_rule\"\nend";

        PipelineDao pipeline = PipelineDao.builder()
                .id("pipeline-1")
                .title("Test Pipeline")
                .source(pipelineSource)
                .build();

        when(pipelineService.loadAll()).thenReturn(Collections.singletonList(pipeline));

        // Create delete event for non-referenced rule
        RulesChangedEvent event = RulesChangedEvent.deletedRule("rule-1", "non_existent_rule");

        // Execute
        updater.handleRuleChanges(event);

        // Verify that pipeline service was NOT called to save
        verify(pipelineService, never()).save(any(), anyBoolean());
    }
}

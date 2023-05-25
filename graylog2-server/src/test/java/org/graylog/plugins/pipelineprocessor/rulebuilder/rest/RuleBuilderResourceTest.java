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
package org.graylog.plugins.pipelineprocessor.rulebuilder.rest;

import org.graylog.plugins.pipelineprocessor.rest.PipelineRuleService;
import org.graylog.plugins.pipelineprocessor.rest.RuleResource;
import org.graylog.plugins.pipelineprocessor.rest.RuleSource;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilder;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilderRegistry;
import org.graylog.plugins.pipelineprocessor.rulebuilder.parser.RuleBuilderService;
import org.graylog.plugins.pipelineprocessor.rulebuilder.parser.validation.ValidatorService;
import org.graylog.plugins.pipelineprocessor.simulator.RuleSimulator;
import org.graylog2.database.NotFoundException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RuleBuilderResourceTest {

    @Mock
    RuleBuilderRegistry ruleBuilderRegistry;
    @Mock
    RuleResource ruleResource;
    @Mock
    RuleBuilderService ruleBuilderService;

    @Mock
    ValidatorService validatorService;
    @Mock
    RuleSimulator ruleSimulator;
    @Mock
    PipelineRuleService pipelineRuleService;

    RuleBuilderResource ruleBuilderResource;

    @Before
    public void setUp() {
        ruleBuilderResource = new RuleBuilderResource(ruleBuilderRegistry, ruleResource, ruleBuilderService, validatorService, ruleSimulator, pipelineRuleService);
    }

    @Test
    public void ruleParsedAndStoredByRuleResource() {
        when(ruleBuilderService.generateRuleSource(any(), any(RuleBuilder.class), anyBoolean()))
                .thenReturn("rulesource");
        when(ruleResource.createFromParser(any())).thenReturn(RuleSource.builder().id("new_id").source("rulesource").build());
        RuleBuilder ruleBuilder = RuleBuilder.builder().build();
        when(ruleBuilderService.generateTitles(any())).thenReturn(ruleBuilder);
        RuleBuilderDto toSave = RuleBuilderDto.builder()
                .title("title")
                .ruleBuilder(ruleBuilder)
                .build();
        final RuleBuilderDto saved = ruleBuilderResource.createFromBuilder(toSave);
        assertThat(saved.id()).isEqualTo("new_id");
        verify(ruleResource).createFromParser(any());
    }

    @Test
    public void ruleParsedAndUpdatedByRuleResource() throws NotFoundException {
        when(ruleBuilderService.generateRuleSource(any(), any(RuleBuilder.class), anyBoolean()))
                .thenReturn("rulesource");
        final String updatedId = "updated_id";
        when(ruleResource.update(eq(updatedId), any(RuleSource.class))).thenReturn(RuleSource.builder().id(updatedId).source("rulesource").build());
        RuleBuilder ruleBuilder = RuleBuilder.builder().build();
        when(ruleBuilderService.generateTitles(any())).thenReturn(ruleBuilder);
        RuleBuilderDto toSave = RuleBuilderDto.builder()
                .title("title")
                .ruleBuilder(ruleBuilder)
                .build();
        final RuleBuilderDto saved = ruleBuilderResource.updateFromBuilder(updatedId, toSave);
        assertThat(saved.id()).isEqualTo(updatedId);
        verify(ruleResource).update(eq(updatedId), any());
    }


}

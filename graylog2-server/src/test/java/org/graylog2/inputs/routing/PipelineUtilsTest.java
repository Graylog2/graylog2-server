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
package org.graylog2.inputs.routing;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.BadRequestException;
import org.apache.commons.io.IOUtils;
import org.graylog.plugins.pipelineprocessor.db.PipelineDao;
import org.graylog.plugins.pipelineprocessor.db.PipelineService;
import org.graylog.plugins.pipelineprocessor.db.RuleDao;
import org.graylog.plugins.pipelineprocessor.db.RuleService;
import org.graylog.plugins.pipelineprocessor.parser.FunctionRegistry;
import org.graylog.plugins.pipelineprocessor.parser.PipelineRuleParser;
import org.graylog.plugins.pipelineprocessor.rest.PipelineSource;
import org.graylog.plugins.pipelineprocessor.rest.PipelineUtils;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.entities.DefaultEntityScope;
import org.graylog2.database.entities.DeletableSystemScope;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PipelineUtilsTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

    @Mock
    private PipelineService pipelineService;

    @Mock
    private RuleService ruleService;

    final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    @Test
    public void createPipelineString() throws IOException {
        final PipelineSource pipelineSource = loadFixture("org/graylog2/inputs/routing/InputRoutingPipelineSource1.json");

        String result = PipelineUtils.createPipelineString(pipelineSource);

        assertThat(result).isEqualTo(pipelineSource.source());
    }

    @Test
    public void updatePipeline() throws Exception {
        final String pipelineId = "pipelineId";
        final PipelineSource pipelineSource = loadFixture("org/graylog2/inputs/routing/InputRoutingPipelineSource1.json");
        final PipelineRuleParser parser = new PipelineRuleParser(mock(FunctionRegistry.class));
        createPipeline(pipelineId);
        createRule(false);

        try (final MockedStatic<PipelineSource> pipelineSourceMockedStatic = mockStatic(PipelineSource.class)) {
            pipelineSourceMockedStatic.when(() -> PipelineSource.fromDao(any(), any())).thenReturn(pipelineSource);

            PipelineUtils.update(pipelineService, parser, ruleService, pipelineId, pipelineSource, true);

            verify(pipelineService).save(argThat(dao -> {
                assertThat(dao.title()).isEqualTo(pipelineSource.title());
                assertThat(dao.description()).isEqualTo(pipelineSource.description());
                assertThat(dao.source()).isEqualTo(pipelineSource.source());
                return true;
            }), anyBoolean());
        }
    }

    @Test(expected = BadRequestException.class)
    public void throwWithUnexpectedSystemRule() throws Exception {
        final String pipelineId = "pipelineId";
        final PipelineSource pipelineSource = loadFixture("org/graylog2/inputs/routing/InputRoutingPipelineSource1.json");
        final PipelineRuleParser parser = new PipelineRuleParser(mock(FunctionRegistry.class));
        createPipeline(pipelineId);
        createRule(true);

        try (final MockedStatic<PipelineSource> pipelineSourceMockedStatic = mockStatic(PipelineSource.class)) {
            pipelineSourceMockedStatic.when(() -> PipelineSource.fromDao(any(), any())).thenReturn(pipelineSource);
            PipelineUtils.update(pipelineService, parser, ruleService, pipelineId, pipelineSource, true);
        }
    }

    private PipelineSource loadFixture(String name) throws IOException {
        String pipelineSourceFixture = IOUtils.toString(Objects.requireNonNull(
                getClass().getClassLoader().getResourceAsStream(name)), StandardCharsets.UTF_8);
        return objectMapper.readValue(pipelineSourceFixture, PipelineSource.class);
    }

    private void createRule(boolean isSystemRule) throws NotFoundException {
        RuleDao ruleDao = RuleDao.builder()
                .id("ruleDaoId")
                .title("ruleDao title")
                .description("ruleDao description")
                .source("ruleDao source")
                .scope(isSystemRule ? DeletableSystemScope.NAME : DefaultEntityScope.NAME)
                .build();
        when(ruleService.loadByName(anyString())).thenReturn(ruleDao);
    }

    private void createPipeline(String id) throws NotFoundException {
        PipelineDao pipelineDao = PipelineDao.builder()
                .id(id)
                .title("pipelineDao title")
                .description("pipelineDao description")
                .source("pipelineDao source")
                .build();
        when(pipelineService.load(id)).thenReturn(pipelineDao);
    }
}

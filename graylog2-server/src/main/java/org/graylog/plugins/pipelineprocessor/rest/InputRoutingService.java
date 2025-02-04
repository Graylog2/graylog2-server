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

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import jakarta.inject.Inject;
import org.graylog.plugins.pipelineprocessor.db.PipelineDao;
import org.graylog.plugins.pipelineprocessor.db.PipelineService;
import org.graylog.plugins.pipelineprocessor.db.RuleDao;
import org.graylog.plugins.pipelineprocessor.db.RuleService;
import org.graylog2.database.NotFoundException;
import org.graylog2.inputs.Input;
import org.graylog2.inputs.InputService;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.rest.resources.system.inputs.InputRenamedEvent;
import org.graylog2.streams.StreamService;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.Optional;

import static org.graylog.plugins.pipelineprocessor.rest.PipelineResource.GL_INPUT_ROUTING_PIPELINE;
import static org.graylog2.shared.utilities.StringUtils.f;

public class InputRoutingService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(InputRoutingService.class);
    public static final String GL_ROUTING_RULE_PREFIX = "gl_route_";

    private final RuleService ruleService;
    private final InputService inputService;
    private final StreamService streamService;
    private final PipelineService pipelineService;

    @Inject
    public InputRoutingService(
            RuleService ruleService,
            InputService inputService,
            StreamService streamService,
            PipelineService pipelineService,
            EventBus eventBus) {
        this.ruleService = ruleService;
        this.inputService = inputService;
        this.streamService = streamService;
        this.pipelineService = pipelineService;

        eventBus.register(this);
    }

    public RuleDao createRoutingRule(PipelineResource.RoutingRequest request) throws NotFoundException {
        Stream stream;
        try {
            stream = streamService.load(request.streamId());
        } catch (NotFoundException e) {
            throw new NotFoundException(f("Unable to load stream %s", request.streamId()), e);
        }

        boolean removeFromDefault = true;
        if (request.removeFromDefault() == null) {
            removeFromDefault = stream.getRemoveMatchesFromDefaultStream();
        }

        Input input;
        try {
            input = inputService.find(request.inputId());
        } catch (NotFoundException e) {
            throw new NotFoundException(f("Unable to load input %s", request.inputId()), e);
        }
        String ruleName = getSystemRuleName(input.getTitle(), stream.getTitle());

        final Optional<RuleDao> ruleDaoOpt = ruleService.findByName(ruleName);
        if (ruleDaoOpt.isPresent()) {
            log.info(f("Routing rule %s already exists - skipping", ruleName));
            return ruleDaoOpt.get();
        }

        String ruleSource =
                "rule \"" + ruleName + "\"\n"
                        + "when has_field(\"gl2_source_input\") AND to_string($message.gl2_source_input)==\"" + request.inputId() + "\"\n"
                        + "then\n"
                        + "route_to_stream(id:\"" + request.streamId() + "\""
                        + ", remove_from_default: " + removeFromDefault
                        + ");\nend\n";

        RuleDao ruleDao = RuleDao.builder()
                .title(ruleName)
                .description("Input setup wizard routing rule")
                .source(ruleSource)
                .createdAt(DateTime.now(DateTimeZone.UTC))
                .build();
        return ruleService.save(ruleDao);
    }

    private String getSystemRuleName(String inputName, String streamName) {
        return GL_ROUTING_RULE_PREFIX + inputName + "_to_" + streamName;
    }

    private boolean isSystemRulePattern(String ruleName) {
        return ruleName.matches(GL_ROUTING_RULE_PREFIX + ".*_to_.*");
    }

    private boolean isSystemRulePattern(String ruleName, String inputName) {
        return ruleName.matches(GL_ROUTING_RULE_PREFIX + inputName + "_to_.*");
    }

    @Subscribe
    public void handleInputRenamed(InputRenamedEvent event) {
        ruleService.loadAll().stream()
                .filter(ruleDao -> isSystemRulePattern(ruleDao.title(), event.oldTitle()))
                .forEach(ruleDao -> {
                    String oldRuleTitle = ruleDao.title();
                    String newRuleTitle = ruleDao.title().replace(event.oldTitle(), event.newTitle());
                    String newSource = ruleDao.source().replace(oldRuleTitle, newRuleTitle);
                    ruleService.save(ruleDao.toBuilder().title(newRuleTitle).source(newSource).build());
                    handleRuleRenamed(oldRuleTitle, newRuleTitle);
                });
    }

    /**
     * Update default pipeline when a rule is renamed.
     * Generally, this is an expensive operation since pipelines are stored as strings and we have no mapping from rules
     * to pipelines that reference them. We therefore limit this operation to system routing rules in the default pipeline.
     */
    private void handleRuleRenamed(String oldTitle, String newTitle) {
        if (!isSystemRulePattern(oldTitle)) {
            return;
        }
        if (!isSystemRulePattern(newTitle)) {
            log.warn("New rule name {} unexpectedly not matching naming pattern", newTitle);
        }
        try {
            PipelineDao pipelineDao = pipelineService.loadByName(GL_INPUT_ROUTING_PIPELINE);
            String pipelineSource = pipelineDao.source().replace(oldTitle, newTitle);
            pipelineService.save(pipelineDao.toBuilder().source(pipelineSource).build());
        } catch (NotFoundException e) {
            log.warn("Unable to load pipeline {}", GL_INPUT_ROUTING_PIPELINE, e);
        }
    }

}

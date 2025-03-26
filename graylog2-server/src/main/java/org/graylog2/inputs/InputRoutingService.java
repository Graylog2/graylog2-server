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
package org.graylog2.inputs;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import jakarta.inject.Inject;
import org.graylog.plugins.pipelineprocessor.db.PipelineDao;
import org.graylog.plugins.pipelineprocessor.db.PipelineService;
import org.graylog.plugins.pipelineprocessor.db.RuleDao;
import org.graylog.plugins.pipelineprocessor.db.RuleService;
import org.graylog.plugins.pipelineprocessor.events.RulesChangedEvent;
import org.graylog.plugins.pipelineprocessor.parser.PipelineRuleParser;
import org.graylog.plugins.pipelineprocessor.rest.PipelineResource;
import org.graylog.plugins.pipelineprocessor.rest.PipelineSource;
import org.graylog.plugins.pipelineprocessor.rest.PipelineUtils;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.entities.DeletableSystemScope;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.rest.resources.system.inputs.InputDeletedEvent;
import org.graylog2.rest.resources.system.inputs.InputRenamedEvent;
import org.graylog2.streams.StreamService;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.graylog.plugins.pipelineprocessor.rest.PipelineResource.GL_INPUT_ROUTING_PIPELINE;
import static org.graylog2.shared.utilities.StringUtils.f;

public class InputRoutingService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(InputRoutingService.class);
    private static final String GL_ROUTING_RULE_PREFIX = "gl_route_";
    private static final Pattern GL_ROUTING_RULE_REGEX = Pattern.compile(GL_ROUTING_RULE_PREFIX + "(.+)\\[(\\w+)\\]_to_(.+)");

    private final RuleService ruleService;
    private final InputService inputService;
    private final StreamService streamService;
    private final PipelineService pipelineService;
    private final PipelineRuleParser pipelineRuleParser;

    @Inject
    public InputRoutingService(
            RuleService ruleService,
            InputService inputService,
            StreamService streamService,
            PipelineService pipelineService,
            PipelineRuleParser pipelineRuleParser,
            EventBus eventBus) {
        this.ruleService = ruleService;
        this.inputService = inputService;
        this.streamService = streamService;
        this.pipelineService = pipelineService;
        this.pipelineRuleParser = pipelineRuleParser;

        eventBus.register(this);
    }

    /**
     * Create a routing rule for the given input and stream ID.
     * Note that the rule title includes the name of the input and stream. If either are renamed, rule references are invalidated.
     */
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
        String ruleName = getSystemRuleName(input.getTitle(), input.getId(), stream.getTitle());

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
                .scope(DeletableSystemScope.NAME)
                .title(ruleName)
                .description("Input setup wizard routing rule")
                .source(ruleSource)
                .createdAt(DateTime.now(DateTimeZone.UTC))
                .build();
        return ruleService.save(ruleDao);
    }

    // Create a human-readable name for a routing rule
    private String getSystemRuleName(String inputName, String inputId, String streamName) {
        return GL_ROUTING_RULE_PREFIX + sanitize(inputName)
                + "[" + inputId + "]_to_"
                + sanitize(streamName);
    }

    private String sanitize(String s) {
        return s.replace('\"', '*');
    }

    private boolean isSystemRulePattern(String ruleName) {
        Matcher matcher = GL_ROUTING_RULE_REGEX.matcher(ruleName);
        return matcher.matches();
    }

    private String createSystemRuleRegex(String inputId, String inputName) {
        return GL_ROUTING_RULE_PREFIX + Pattern.quote(sanitize(inputName)) + "\\[" + inputId + "\\]_to_.*";
    }

    private String replaceInputName(String ruleName, String oldInputName, String newInputName) {
        Matcher matcher = GL_ROUTING_RULE_REGEX.matcher(ruleName);
        if (matcher.matches()) {
            String inputName = matcher.group(1); // "input"
            String inputId = matcher.group(2); // "123"
            String streamName = matcher.group(3); // "stream"

            if (inputName.equals(oldInputName)) {
                return getSystemRuleName(newInputName, inputId, streamName);
            }
        }
        throw new IllegalArgumentException("Unexpected rule name not matching naming pattern: " + ruleName);
    }

    /**
     * Update routing rules when an input is renamed.
     */
    @Subscribe
    public void handleInputRenamed(InputRenamedEvent event) {
        ruleService.loadAllByTitle(createSystemRuleRegex(event.inputId(), event.oldInputTitle()))
                .forEach(ruleDao -> {
                    String oldRuleTitle = ruleDao.title();
                    String newRuleTitle = replaceInputName(oldRuleTitle, sanitize(event.oldInputTitle()), sanitize(event.newInputTitle()));
                    String newSource = ruleDao.source().replace(oldRuleTitle, newRuleTitle);
                    ruleService.save(ruleDao.toBuilder().title(newRuleTitle).source(newSource).build(), false);
                    handleRuleRenamed(oldRuleTitle, newRuleTitle);
                });
    }

    /**
     * Update default pipeline when a routing rule is renamed.
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
            pipelineService.save(pipelineDao.toBuilder().source(pipelineSource).build(), false);
        } catch (NotFoundException e) {
            log.warn("Unable to load pipeline {}", GL_INPUT_ROUTING_PIPELINE, e);
        }
    }

    /**
     * Update routing rules when an input is deleted.
     */
    @Subscribe
    public void handleInputDeleted(InputDeletedEvent event) {
        ruleService.loadAllByTitle(createSystemRuleRegex(event.inputId(), event.inputTitle()))
                .forEach(ruleService::delete);
    }

    @Subscribe
    public void handleRuleDeleted(RulesChangedEvent event) {
        event.deletedRules().stream()
                .map(RulesChangedEvent.Reference::title)
                .filter(this::isSystemRulePattern)
                .forEach(this::deleteFromDefaultPipeline);
    }

    /**
     * Update default pipeline when a routing rule is deleted.
     */
    private void deleteFromDefaultPipeline(String ruleTitle) {
        try {
            PipelineDao pipelineDao = pipelineService.loadByName(GL_INPUT_ROUTING_PIPELINE);
            PipelineSource pipelineSource = PipelineSource.fromDao(pipelineRuleParser, pipelineDao);
            final List<String> rules0 = pipelineSource.stages().get(0).rules();
            rules0.stream()
                    .filter(ruleRef -> ruleRef.equals(ruleTitle))
                    .findFirst()
                    .ifPresent(rules0::remove);
            pipelineSource = pipelineSource.toBuilder()
                    .source(PipelineUtils.createPipelineString(pipelineSource))
                    .build();
            PipelineUtils.update(pipelineService, pipelineRuleParser, ruleService, pipelineDao.id(), pipelineSource, false);
        } catch (NotFoundException e) {
            log.warn("Unable to load pipeline {}", GL_INPUT_ROUTING_PIPELINE, e);
        }
    }
}

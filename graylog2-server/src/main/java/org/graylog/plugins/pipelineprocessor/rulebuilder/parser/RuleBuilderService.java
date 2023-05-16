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
package org.graylog.plugins.pipelineprocessor.rulebuilder.parser;

import com.swrve.ratelimitedlogger.RateLimitedLog;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilder;

import javax.inject.Inject;

import static org.graylog.plugins.pipelineprocessor.processors.PipelineInterpreter.getRateLimitedLog;

public class RuleBuilderService {

    private static final RateLimitedLog log = getRateLimitedLog(RuleBuilderService.class);

    private final String RULE_TEMPLATE =
            "rule \"%s\"" + System.lineSeparator() +
                    "when" + System.lineSeparator() +
                    "%s" + System.lineSeparator() +
                    "then" + System.lineSeparator() +
                    "%s" + System.lineSeparator() +
                    "end";

    private final ConditionParser conditionParser;
    private final ActionParser actionParser;

    @Inject
    public RuleBuilderService(ConditionParser conditionParser, ActionParser actionParser) {
        this.conditionParser = conditionParser;
        this.actionParser = actionParser;
    }

    public String generateRuleSource(String title, RuleBuilder ruleBuilder, boolean generateSimulatorFields) {
        //TODO: possible injection here, sanitize input!
        final String rule = String.format(RULE_TEMPLATE, title,
                conditionParser.generate(ruleBuilder.conditions()),
                actionParser.generate(ruleBuilder.actions(), generateSimulatorFields));
        log.debug(rule);
        return rule;
    }


}

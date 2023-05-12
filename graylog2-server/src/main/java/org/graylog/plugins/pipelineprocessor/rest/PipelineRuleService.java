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

import org.graylog.plugins.pipelineprocessor.ast.Rule;
import org.graylog.plugins.pipelineprocessor.db.RuleDao;
import org.graylog.plugins.pipelineprocessor.parser.ParseException;
import org.graylog.plugins.pipelineprocessor.parser.PipelineRuleParser;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response;

public class PipelineRuleService {

    private final PipelineRuleParser pipelineRuleParser;

    @Inject
    public PipelineRuleService(PipelineRuleParser pipelineRuleParser) {
        this.pipelineRuleParser = pipelineRuleParser;
    }

    public Rule parseRuleOrThrow(String ruleId, String source, boolean silent) {
        try {
            return pipelineRuleParser.parseRule(ruleId, source, silent);
        } catch (ParseException e) {
            throw new BadRequestException(Response.status(Response.Status.BAD_REQUEST).entity(e.getErrors()).build());
        }
    }

    public RuleSource createRuleSourceFromRuleDao(RuleDao ruleDao) {
        return RuleSource.fromDao(pipelineRuleParser, ruleDao);
    }
}

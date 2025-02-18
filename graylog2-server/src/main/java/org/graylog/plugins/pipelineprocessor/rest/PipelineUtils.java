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

import com.swrve.ratelimitedlogger.RateLimitedLog;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;
import org.graylog.plugins.pipelineprocessor.ast.Pipeline;
import org.graylog.plugins.pipelineprocessor.db.PipelineDao;
import org.graylog.plugins.pipelineprocessor.db.PipelineService;
import org.graylog.plugins.pipelineprocessor.db.RuleDao;
import org.graylog.plugins.pipelineprocessor.db.RuleService;
import org.graylog.plugins.pipelineprocessor.db.SystemPipelineRuleScope;
import org.graylog.plugins.pipelineprocessor.parser.ParseException;
import org.graylog.plugins.pipelineprocessor.parser.PipelineRuleParser;
import org.graylog2.database.NotFoundException;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import static org.graylog.plugins.pipelineprocessor.processors.PipelineInterpreter.getRateLimitedLog;

/**
 * Factor out utility methods that are used in multiple services to avoid circular dependencies.
 */
public class PipelineUtils {
    private static final RateLimitedLog log = getRateLimitedLog(PipelineUtils.class);

    private PipelineUtils() {
    }

    public static PipelineSource update(PipelineService pipelineService,
                                        PipelineRuleParser pipelineRuleParser,
                                        RuleService ruleService,
                                        String id,
                                        PipelineSource update,
                                        boolean checkMutability) throws NotFoundException {
        final PipelineDao dao = pipelineService.load(id);
        final Pipeline pipeline;
        try {
            pipeline = pipelineRuleParser.parsePipeline(update.id(), update.source());
        } catch (ParseException e) {
            throw new BadRequestException(Response.status(Response.Status.BAD_REQUEST).entity(e.getErrors()).build());
        }
        if (checkMutability) {
            checkSystemRules(ruleService, pipeline);
        }

        final PipelineDao toSave = dao.toBuilder()
                .title(pipeline.name())
                .description(update.description())
                .source(update.source())
                .modifiedAt(DateTime.now(DateTimeZone.UTC))
                .build();

        final PipelineDao savedPipeline;
        try {
            savedPipeline = pipelineService.save(toSave, checkMutability);
        } catch (IllegalArgumentException e) {
            log.error(e.getMessage(), e);
            throw new BadRequestException(e.getMessage());
        }

        return PipelineSource.fromDao(pipelineRuleParser, savedPipeline);
    }

    private static void checkSystemRules(RuleService ruleService, Pipeline pipeline) {
        pipeline.stages().stream()
                .flatMap(stage -> stage.ruleReferences().stream())
                .filter(ruleRef -> isSystemRule(ruleService, ruleRef))
                .findAny()
                .ifPresent(rule -> {
                    throw new BadRequestException("System rules cannot be assigned to other pipelines.");
                });
    }

    private static boolean isSystemRule(RuleService ruleService, String ruleRef) {
        try {
            final RuleDao ruleDao = ruleService.loadByName(ruleRef);
            return ruleDao.scope().equalsIgnoreCase(SystemPipelineRuleScope.NAME);
        } catch (NotFoundException e) {
            return false;
        }
    }

    public static String createPipelineString(PipelineSource pipelineSource) {
        StringBuilder result = new StringBuilder("pipeline \"" + pipelineSource.title() + "\"\n");
        for (int stageNr = 0; stageNr < pipelineSource.stages().size(); stageNr++) {
            StageSource currStage = pipelineSource.stages().get(stageNr);
            result.append("stage ").append(stageNr).append(" match ").append(currStage.match()).append('\n');
            for (String rule : currStage.rules()) {
                result.append("rule \"").append(rule).append("\"\n");
            }
        }
        result.append("end");

        return result.toString();
    }

}

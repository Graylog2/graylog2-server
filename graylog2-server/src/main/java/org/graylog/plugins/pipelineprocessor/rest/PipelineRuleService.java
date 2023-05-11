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

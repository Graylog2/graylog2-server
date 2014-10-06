package org.graylog2.restclient.models.api.responses.alerts;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class ConditionTriggeredSummary {
    public AlertConditionSummaryResponse condition;
    public Boolean triggered;
}

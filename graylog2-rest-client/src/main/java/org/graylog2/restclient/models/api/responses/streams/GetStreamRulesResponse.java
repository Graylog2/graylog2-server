package org.graylog2.restclient.models.api.responses.streams;

import java.util.List;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class GetStreamRulesResponse {
    public int total;
    public List<StreamRuleSummaryResponse> stream_rules;
}

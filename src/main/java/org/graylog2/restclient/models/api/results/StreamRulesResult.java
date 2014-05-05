package org.graylog2.restclient.models.api.results;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import org.graylog2.restclient.models.StreamRule;
import org.graylog2.restclient.models.api.responses.streams.StreamRuleSummaryResponse;

import java.util.List;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class StreamRulesResult {

    private final int total;
    private final List<StreamRuleSummaryResponse> streamRules;

    public StreamRulesResult(int total, List<StreamRuleSummaryResponse> streamRules) {
        this.total = total;
        this.streamRules = streamRules;
    }

    public int getTotal() {
        return total;
    }

    public List<StreamRule> getStreamRules() {
        List<StreamRule> streamRules = Lists.newArrayList();

        /*for (StreamRuleSummaryResponse srsr : this.streamRules) {
            streamRules.add(streamRuleFactory.fromSummaryResponse(srsr));
        }*/

        return streamRules;
    }
}

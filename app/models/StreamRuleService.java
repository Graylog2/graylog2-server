package models;

import com.google.inject.Inject;
import lib.APIException;
import lib.ApiClient;
import models.api.requests.streams.CreateStreamRuleRequest;
import models.api.responses.streams.CreateStreamRuleResponse;
import models.api.responses.streams.GetStreamRulesResponse;
import models.api.responses.streams.StreamRuleSummaryResponse;
import models.api.results.StreamRulesResult;
import play.mvc.Http;

import java.io.IOException;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class StreamRuleService {
    private final ApiClient api;

    private final StreamRule.Factory streamRuleFactory;

    @Inject
    private StreamRuleService(ApiClient api, StreamRule.Factory streamRuleFactory) {
        this.api = api;
        this.streamRuleFactory = streamRuleFactory;
    }

    public StreamRulesResult all(String streamId) throws IOException, APIException {
        GetStreamRulesResponse r;
        r = api.get(GetStreamRulesResponse.class).path("/streams/"+streamId+"/rules").execute();

        return new StreamRulesResult(r.total, r.stream_rules);
    }

    public StreamRule get(String streamId, String streamRuleId) throws IOException, APIException {
        StreamRuleSummaryResponse streamRuleResponse = null;
        streamRuleResponse = api.get(StreamRuleSummaryResponse.class).path("/streams/"+streamId+"/rules/"+streamRuleId).expect(Http.Status.OK).execute();

        return streamRuleFactory.fromSummaryResponse(streamRuleResponse);
    }

    public CreateStreamRuleResponse create(String streamId, CreateStreamRuleRequest request) throws APIException, IOException {
        return api.post(CreateStreamRuleResponse.class).path("/streams/" + streamId + "/rules").body(request).expect(Http.Status.CREATED).execute();
    }

    public CreateStreamRuleResponse update(String streamId, String streamRuleId, CreateStreamRuleRequest request) throws APIException, IOException {
        return api.post(CreateStreamRuleResponse.class).path("/streams/" + streamId + "/rules/" + streamRuleId).body(request).expect(Http.Status.OK).execute();
    }

    public void delete(String streamId, String streamRuleId) throws APIException, IOException {
        api.delete().path("/streams/" + streamId + "/rules/" + streamRuleId).expect(Http.Status.NO_CONTENT).execute();
    }
}

package models;

import com.google.inject.Inject;
import lib.APIException;
import lib.ApiClient;
import models.api.requests.streams.CreateStreamRequest;
import models.api.requests.streams.CreateStreamRuleRequest;
import models.api.responses.streams.CreateStreamRuleResponse;
import models.api.responses.streams.GetStreamRulesResponse;
import models.api.responses.streams.StreamRuleSummaryResponse;
import models.api.results.StreamRulesResult;
import play.mvc.Http;

import java.io.IOException;
import java.util.List;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class StreamRuleService {
    private final ApiClient api;

    @Inject
    private StreamRuleService(ApiClient api) {
        this.api = api;
    }

    public StreamRulesResult all(String streamId) throws IOException, APIException {
        GetStreamRulesResponse r;
        r = api.get(GetStreamRulesResponse.class).path("/streams/"+streamId+"/rules").execute();

        return new StreamRulesResult(r.total, r.stream_rules);
    }

    public CreateStreamRuleResponse create(String streamId, CreateStreamRuleRequest request) throws APIException, IOException {
        return api.post(CreateStreamRuleResponse.class).path("/streams/" + streamId + "/rules").body(request).expect(Http.Status.CREATED).execute();
    }

    public void delete(String streamId, String streamRuleId) throws APIException, IOException {
        api.delete().path("/streams/" + streamId + "/rules/" + streamRuleId).expect(Http.Status.NO_CONTENT).execute();
    }
}

package controllers.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import controllers.AuthenticatedController;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.models.StreamRule;
import org.graylog2.restclient.models.StreamRuleService;
import org.graylog2.restclient.models.api.requests.streams.CreateStreamRuleRequest;
import org.graylog2.restclient.models.api.responses.streams.CreateStreamRuleResponse;
import org.graylog2.restclient.models.api.results.StreamRulesResult;
import play.libs.Json;
import play.mvc.Result;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;

public class StreamRulesApiController extends AuthenticatedController {
    public class Type {
        public final int id;
        public final String shortDesc;
        public final String longDesc;

        public Type(int id, String shortDesc, String longDesc) {
            this.id = id;
            this.shortDesc = shortDesc;
            this.longDesc = longDesc;
        }
    }

    private final StreamRuleService streamRuleService;

    @Inject
    public StreamRulesApiController(StreamRuleService streamRuleService) {
        this.streamRuleService = streamRuleService;
    }

    public Result types() {
        final List<Type> types = Lists.newArrayList();
        for (StreamRule.Type type : StreamRule.Type.values()) {
            types.add(new Type(type.getId(), type.getShortDesc(), type.getLongDesc()));
        }
        return ok(Json.toJson(types));
    }

    public Result list(String streamId) throws IOException, APIException {
        final StreamRulesResult result = streamRuleService.all(streamId);
        return ok(Json.toJson(result));
    }

    public Result update(String streamId, String streamRuleId) throws APIException, IOException {
        final JsonNode json = request().body().asJson();
        final CreateStreamRuleRequest request = Json.fromJson(json, CreateStreamRuleRequest.class);
        streamRuleService.update(streamId, streamRuleId, request);
        return ok();
    }

    public Result delete(String streamId, String streamRuleId) throws APIException, IOException {
        streamRuleService.delete(streamId, streamRuleId);
        return ok();
    }

    public Result create(String streamId) throws APIException, IOException {
        final JsonNode json = request().body().asJson();
        final CreateStreamRuleRequest request = Json.fromJson(json, CreateStreamRuleRequest.class);
        final CreateStreamRuleResponse result = streamRuleService.create(streamId, request);
        return ok(Json.toJson(result));
    }
}

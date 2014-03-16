package controllers.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.inject.Inject;
import controllers.AuthenticatedController;
import lib.APIException;
import lib.ApiClient;
import lib.Tools;
import models.*;
import models.api.requests.streams.TestMatchRequest;
import models.api.responses.streams.TestMatchResponse;
import play.data.Form;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;

import java.io.IOException;
import java.util.Map;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 *         Lennart Koopmann <lennart@torch.sh>
 */
public class StreamsApiController extends AuthenticatedController {

    @Inject
    StreamService streamService;

    @BodyParser.Of(BodyParser.Json.class)
    public Result testMatch(String stream_id) {
        JsonNode json = request().body().asJson();

        ObjectMapper mapper = new ObjectMapper();
        TestMatchResponse response = null;

        try {
            TestMatchRequest tmr = mapper.readValue(json.toString(), TestMatchRequest.class);
            response = streamService.testMatch(stream_id, tmr);
        } catch (APIException e) {
            String message = "Could not test stream rule matching. We expected HTTP 201, but got a HTTP " + e.getHttpCode() + ".";
            return status(504, message);
        } catch (IOException e) {
            return status(504, e.toString());
        }

        return ok(new Gson().toJson(response)).as("application/json");
    }

}

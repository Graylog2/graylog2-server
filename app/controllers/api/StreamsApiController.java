package controllers.api;

import controllers.AuthenticatedController;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.models.Stream;
import org.graylog2.restclient.models.StreamService;
import org.graylog2.restclient.models.api.requests.streams.TestMatchRequest;
import org.graylog2.restclient.models.api.responses.streams.TestMatchResponse;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Result;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;

public class StreamsApiController extends AuthenticatedController {
    private final StreamService streamService;

    @Inject
    public StreamsApiController(StreamService streamService) {
        this.streamService = streamService;
    }

    public Result list() throws IOException, APIException {
        final List<Stream> streams  = this.streamService.all();

        return ok(Json.toJson(streams));
    }

    public Result delete(String streamId) throws APIException, IOException {
        this.streamService.delete(streamId);
        return ok();
    }

    @BodyParser.Of(BodyParser.Json.class)
    public Result testMatch(String stream_id) {

        TestMatchResponse response = null;
        try {
            TestMatchRequest tmr = Json.fromJson(request().body().asJson(), TestMatchRequest.class);
            response = streamService.testMatch(stream_id, tmr);
        } catch (APIException e) {
            String message = "Could not test stream rule matching. We expected HTTP 201, but got a HTTP " + e.getHttpCode() + ".";
            return status(504, message);
        } catch (IOException e) {
            return status(504, e.toString());
        }

        return ok(Json.toJson(response));
    }
}

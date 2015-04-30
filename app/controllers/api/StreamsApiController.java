package controllers.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import controllers.AuthenticatedController;
import controllers.SearchControllerV2;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.models.Stream;
import org.graylog2.restclient.models.StreamService;
import org.graylog2.restclient.models.api.requests.streams.CreateStreamRequest;
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

    public Result create() throws APIException, IOException {
        final JsonNode json = request().body().asJson();
        final CreateStreamRequest request = Json.fromJson(json, CreateStreamRequest.class);

        return ok(Json.toJson(this.streamService.create(request)));
    }

    public Result resume(String streamId) throws APIException, IOException {
        this.streamService.resume(streamId);
        return ok();
    }

    public Result update(String streamId) throws APIException, IOException {
        final JsonNode json = request().body().asJson();
        final CreateStreamRequest request = Json.fromJson(json, CreateStreamRequest.class);

        this.streamService.update(streamId, request);
        return ok();
    }

    public Result cloneStream(String streamId) throws APIException, IOException {
        final JsonNode json = request().body().asJson();
        final CreateStreamRequest request = Json.fromJson(json, CreateStreamRequest.class);

        this.streamService.cloneStream(streamId, request);
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

    public Result listStreams() {
        List<SearchControllerV2.StreamDescription> streamDescriptions = Lists.newArrayList();
        try {
            final List<Stream> streams = streamService.all();
            for (Stream stream : streams) {
                streamDescriptions.add(new SearchControllerV2.StreamDescription(stream));
            }
        } catch (IOException e) {
            return status(500, "Could not load streams");
        } catch (APIException e) {
            return status(500, "Could not load streams, received HTTP " + e.getHttpCode() + ": " + e.getMessage());
        }

        return ok(Json.toJson(streamDescriptions));
    }
}

package controllers;

import lib.BreadcrumbList;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.lib.ApiClient;
import org.graylog2.restclient.models.Stream;
import org.graylog2.restclient.models.StreamRule;
import org.graylog2.restclient.models.StreamRuleService;
import org.graylog2.restclient.models.StreamService;
import org.graylog2.restclient.models.api.requests.streams.CreateStreamRuleRequest;
import org.graylog2.restclient.models.api.responses.streams.CreateStreamRuleResponse;
import play.data.Form;
import play.mvc.Result;

import javax.inject.Inject;
import java.io.IOException;

public class StreamRulesController extends AuthenticatedController {
    private final StreamService streamService;

    @Inject
    public StreamRulesController(StreamService streamService) {
        this.streamService = streamService;
    }

    public Result index(String streamId) {
        Stream stream;
        try {
            stream = streamService.get(streamId);
        } catch (APIException e) {
            String message = "Could not fetch stream rules. We expect HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(504, views.html.errors.error.render(message, e, request()));
        } catch (IOException e) {
            return status(504, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        }

        return ok(views.html.streamrules.index.render(currentUser(), stream, stream.getStreamRules()));
    }
}

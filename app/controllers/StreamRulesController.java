package controllers;

import com.google.inject.Inject;
import lib.APIException;
import lib.ApiClient;
import models.Stream;
import models.StreamRuleService;
import models.StreamService;
import models.api.requests.streams.CreateStreamRuleRequest;
import play.data.Form;
import play.mvc.Result;

import java.io.IOException;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class StreamRulesController extends AuthenticatedController {
    private static final Form<CreateStreamRuleRequest> createStreamRuleForm = Form.form(CreateStreamRuleRequest.class);

    @Inject
    private StreamService streamService;

    @Inject
    private StreamRuleService streamRuleService;

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

    public Result create(String streamId) {
        Form<CreateStreamRuleRequest> form = createStreamRuleForm.bindFromRequest();

        try {
            CreateStreamRuleRequest csrr = form.get();
            streamRuleService.create(streamId, csrr);
        } catch (APIException e) {
            String message = "Could not create stream rule. We expected HTTP 201, but got a HTTP " + e.getHttpCode() + ".";
            return status(504, message);
        } catch (IOException e) {
            return status(504, e.toString());
        }
        return ok();
    }

    public Result delete(String streamId, String streamRuleId){
        try {
            streamRuleService.delete(streamId, streamRuleId);
        } catch (APIException e) {
            String message = "Could not delete stream rule. We expect HTTP 204, but got a HTTP " + e.getHttpCode() + ".";
            return status(504, views.html.errors.error.render(message, e, request()));
        } catch (IOException e) {
            return status(504, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        }

        return ok();
    }
}

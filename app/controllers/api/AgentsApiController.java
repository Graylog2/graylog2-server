package controllers.api;

import controllers.AuthenticatedController;
import org.graylog2.rest.models.agent.responses.AgentSummary;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.models.AgentService;
import play.libs.Json;
import play.mvc.Result;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;

public class AgentsApiController extends AuthenticatedController {
    private final AgentService agentService;

    @Inject
    public AgentsApiController(AgentService agentService) {
        this.agentService = agentService;
    }

    public Result index() throws APIException, IOException {
        final List<AgentSummary> agents = agentService.all();

        return ok(Json.toJson(agents));
    }
}

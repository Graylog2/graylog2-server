package controllers;

import lib.BreadcrumbList;
import org.graylog2.rest.models.agent.responses.AgentSummary;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.models.AgentService;
import play.mvc.Result;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;

public class AgentsController extends AuthenticatedController {
    private final AgentService agentService;

    @Inject
    public AgentsController(AgentService agentService) {
        this.agentService = agentService;
    }

    public Result index() throws APIException, IOException {
        final List<AgentSummary> agents = agentService.all();

        BreadcrumbList bc = new BreadcrumbList();
        bc.addCrumb("System", routes.SystemController.index(0));
        bc.addCrumb("Agents", routes.AgentsController.index());

        return ok(views.html.system.agents.index.render(currentUser(), bc, agents));
    }
}

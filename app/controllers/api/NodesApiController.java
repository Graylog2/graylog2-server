package controllers.api;

import com.google.common.collect.Lists;
import controllers.AuthenticatedController;
import models.descriptions.NodeDescription;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.lib.ServerNodes;
import org.graylog2.restclient.models.Node;
import org.graylog2.restclient.models.NodeService;
import org.graylog2.restclient.models.Radio;
import play.libs.Json;
import play.mvc.Result;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;

public class NodesApiController extends AuthenticatedController {
    private final ServerNodes serverNodes;
    private final NodeService nodeService;

    @Inject
    public NodesApiController(ServerNodes serverNodes, NodeService nodeService) {
        this.serverNodes = serverNodes;
        this.nodeService = nodeService;
    }

    public Result nodes() {
        final List<NodeDescription> nodes = Lists.newArrayList();

        for (final Node node : serverNodes.all()) {
            nodes.add(new NodeDescription(node));
        }

        try {
            for (final Radio radio : nodeService.radios().values()) {
                nodes.add(new NodeDescription(radio));
            }
        } catch (IOException e) {
            return status(504, e.getMessage());
        } catch (APIException e) {
            String message = "Could not fetch radio information. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(504, message);
        }

        return ok(Json.toJson(nodes));
    }
}

package controllers.api;

import controllers.AuthenticatedController;
import org.graylog2.rest.models.collector.responses.CollectorSummary;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.models.CollectorService;
import play.libs.Json;
import play.mvc.Result;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;

public class CollectorsApiController extends AuthenticatedController {
    private final CollectorService collectorService;

    @Inject
    public CollectorsApiController(CollectorService collectorService) {
        this.collectorService = collectorService;
    }

    public Result index() throws APIException, IOException {
        final List<CollectorSummary> collectors = collectorService.all();

        return ok(Json.toJson(collectors));
    }
}

package org.graylog2.restclient.models;

import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.lib.ApiClient;
import org.graylog2.restclient.models.api.requests.outputs.OutputLaunchRequest;
import org.graylog2.restclient.models.api.responses.system.OutputSummaryResponse;
import org.graylog2.restclient.models.api.responses.system.OutputsResponse;
import org.graylog2.restroutes.generated.routes;
import play.mvc.Http;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class OutputService {
    private final ApiClient api;
    private final Output.Factory outputFactory;

    @Inject
    public OutputService(ApiClient api, Output.Factory outputFactory) {
        this.api = api;
        this.outputFactory = outputFactory;
    }

    public List<Output> list() throws APIException, IOException {
        OutputsResponse outputsResponse = api.path(routes.OutputResource().get(), OutputsResponse.class).execute();
        List<Output> result = new ArrayList<>();
        for(OutputSummaryResponse response : outputsResponse.outputs)
            result.add(outputFactory.fromSummaryResponse(response));

        return result;
    }

    public Output create(OutputLaunchRequest request) throws APIException, IOException {
        OutputSummaryResponse response = api.path(routes.OutputResource().create(), OutputSummaryResponse.class)
                .body(request)
                .expect(Http.Status.CREATED)
                .execute();
        return outputFactory.fromSummaryResponse(response);
    }

    public Output get(String outputId) throws APIException, IOException {
        OutputSummaryResponse response = api.path(routes.OutputResource().get(outputId), OutputSummaryResponse.class).execute();
        return outputFactory.fromSummaryResponse(response);
    }

    public void delete(String outputId) throws APIException, IOException {
        api.path(routes.OutputResource().delete(outputId)).execute();
    }

    public OutputTypesResponse available() throws APIException, IOException {
        return api.path(routes.OutputResource().available(), OutputTypesResponse.class).execute();
    }
}

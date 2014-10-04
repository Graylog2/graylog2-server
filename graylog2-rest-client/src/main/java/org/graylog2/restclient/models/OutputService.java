/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
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

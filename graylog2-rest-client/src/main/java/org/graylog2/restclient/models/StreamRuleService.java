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

import com.google.inject.Inject;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.lib.ApiClient;
import org.graylog2.restclient.models.api.requests.streams.CreateStreamRuleRequest;
import org.graylog2.restclient.models.api.responses.streams.CreateStreamRuleResponse;
import org.graylog2.restclient.models.api.responses.streams.GetStreamRulesResponse;
import org.graylog2.restclient.models.api.responses.streams.StreamRuleSummaryResponse;
import org.graylog2.restclient.models.api.results.StreamRulesResult;
import org.graylog2.restroutes.generated.StreamRuleResource;
import org.graylog2.restroutes.generated.routes;
import play.mvc.Http;

import java.io.IOException;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class StreamRuleService {
    private final ApiClient api;

    private final StreamRule.Factory streamRuleFactory;
    private final StreamRuleResource resource = routes.StreamRuleResource();

    @Inject
    private StreamRuleService(ApiClient api, StreamRule.Factory streamRuleFactory) {
        this.api = api;
        this.streamRuleFactory = streamRuleFactory;
    }

    public StreamRulesResult all(String streamId) throws IOException, APIException {
        GetStreamRulesResponse r;
        r = api.path(resource.get(streamId), GetStreamRulesResponse.class).execute();

        return new StreamRulesResult(r.total, r.stream_rules);
    }

    public StreamRule get(String streamId, String streamRuleId) throws IOException, APIException {
        StreamRuleSummaryResponse streamRuleResponse = null;
        streamRuleResponse = api.path(resource.get(streamId, streamRuleId), StreamRuleSummaryResponse.class)
                .expect(Http.Status.OK).execute();

        return streamRuleFactory.fromSummaryResponse(streamRuleResponse);
    }

    public CreateStreamRuleResponse create(String streamId, CreateStreamRuleRequest request) throws APIException, IOException {
        return api.path(resource.create(streamId), CreateStreamRuleResponse.class)
                .body(request).expect(Http.Status.CREATED).execute();
    }

    public CreateStreamRuleResponse update(String streamId, String streamRuleId, CreateStreamRuleRequest request) throws APIException, IOException {
        return api.path(resource.update(streamId, streamRuleId), CreateStreamRuleResponse.class)
                .body(request).expect(Http.Status.OK).execute();
    }

    public void delete(String streamId, String streamRuleId) throws APIException, IOException {
        api.path(resource.delete(streamId, streamRuleId))
                .expect(Http.Status.NO_CONTENT).execute();
    }
}

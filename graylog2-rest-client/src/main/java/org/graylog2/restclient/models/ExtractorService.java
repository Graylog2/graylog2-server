/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.restclient.models;

import com.google.common.collect.Lists;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.lib.ApiClient;
import org.graylog2.restclient.models.api.requests.CreateExtractorRequest;
import org.graylog2.restclient.models.api.requests.ExtractorOrderRequest;
import org.graylog2.restclient.models.api.requests.GrokPatternUpdateRequest;
import org.graylog2.restclient.models.api.responses.system.CreateExtractorResponse;
import org.graylog2.restclient.models.api.responses.system.ExtractorSummaryResponse;
import org.graylog2.restclient.models.api.responses.system.ExtractorsResponse;
import org.graylog2.restclient.models.api.responses.system.GrokPatternResponse;
import org.graylog2.restroutes.generated.ExtractorsResource;
import org.graylog2.restroutes.generated.routes;
import play.mvc.Http;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.SortedMap;

public class ExtractorService {

    private final ApiClient api;
    private final Extractor.Factory extractorFactory;
    private final ExtractorsResource resource = routes.ExtractorsResource();

    @Inject
    private ExtractorService(ApiClient api, Extractor.Factory extractorFactory) {
        this.api = api;
        this.extractorFactory = extractorFactory;
    }

    public String create(Node node, Input input, CreateExtractorRequest request) throws IOException, APIException {
        final CreateExtractorResponse response = api.path(routes.ExtractorsResource().create(input.getId()), CreateExtractorResponse.class)
                .node(node)
                .expect(Http.Status.CREATED)
                .body(request)
                .execute();
        return response.extractorId;
    }

    public void update(String extractorId, Node node, Input input, CreateExtractorRequest request) throws IOException, APIException {
        api.path(resource.update(input.getId(), extractorId))
                .node(node)
                .expect(Http.Status.OK)
                .body(request)
                .execute();
    }

    public void delete(Node node, Input input, String extractorId) throws IOException, APIException {
        api.path(resource.terminate(input.getId(), extractorId))
                .node(node)
                .expect(Http.Status.NO_CONTENT)
                .execute();
    }

    public Extractor load(Node node, Input input, String extractorId) throws IOException, APIException {
        final ExtractorSummaryResponse extractorSummaryResponse = api.path(resource.single(input.getId(), extractorId), ExtractorSummaryResponse.class)
                .node(node)
                .execute();

        return extractorFactory.fromResponse(extractorSummaryResponse);
    }

    public List<Extractor> all(Node node, Input input) throws IOException, APIException {
        List<Extractor> extractors = Lists.newArrayList();

        final ExtractorsResponse extractorsResponse = api.path(resource.list(input.getId()), ExtractorsResponse.class)
                .node(node)
                .execute();
        for (ExtractorSummaryResponse ex : extractorsResponse.extractors) {
            extractors.add(extractorFactory.fromResponse(ex));
        }

        return extractors;
    }

    public void order(String inputId, SortedMap<Integer, String> order) throws APIException, IOException {
        ExtractorOrderRequest req = new ExtractorOrderRequest();
        req.order = order;

        api.path(resource.order(inputId))
                .body(req)
                .onlyMasterNode()
                .execute();
    }
    
    public Collection<GrokPattern> allGrokPatterns() throws APIException, IOException {
        final GrokPatternResponse response = 
                api.path(routes.GrokResource().listGrokPatterns(), GrokPatternResponse.class)
                .expect(Http.Status.OK)
                .execute();
        
        return response.patterns;
    }
    
    public GrokPattern createGrokPattern(GrokPattern pattern) throws APIException, IOException {
        final GrokPattern grokPattern = api.path(routes.GrokResource().createPattern(), GrokPattern.class)
                .body(pattern)
                .expect(Http.Status.CREATED)
                .execute();
        return grokPattern;
    }

    public void updateGrokPattern(GrokPattern pattern) throws APIException, IOException {
        api.path(routes.GrokResource().updatePattern(pattern.id))
                .body(pattern)
                .execute();
    }
    
    public void deleteGrokPattern(GrokPattern pattern) throws APIException, IOException {
        api.path(routes.GrokResource().removePattern(pattern.id)).execute();
    }
    
    public void bulkLoadGrokPatterns(Collection<GrokPattern> patterns, boolean replace) throws APIException, IOException {
        api.path(routes.GrokResource().bulkUpdatePatterns())
                .queryParam("replace", String.valueOf(replace))
                .body(new GrokPatternUpdateRequest(patterns))
                .execute();
        
    }
}

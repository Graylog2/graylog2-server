/**
 * The MIT License
 * Copyright (c) 2012 TORCH GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.graylog2.restclient.models;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.lib.ApiClient;
import org.graylog2.restclient.models.api.requests.ExtractorOrderRequest;
import org.graylog2.restclient.models.api.responses.EmptyResponse;
import org.graylog2.restclient.models.api.responses.system.ExtractorSummaryResponse;
import org.graylog2.restclient.models.api.responses.system.ExtractorsResponse;
import org.graylog2.restroutes.generated.ExtractorsResource;
import org.graylog2.restroutes.generated.routes;
import play.mvc.Http;

import java.io.IOException;
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

    public void delete(Node node, Input input, String extractorId) throws IOException, APIException {
        api.path(resource.terminate(input.getId(), extractorId))
                .node(node)
                .expect(Http.Status.NO_CONTENT)
                .execute();
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

}

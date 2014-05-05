/*
 * Copyright 2013 TORCH UG
 *
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
package models;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import lib.APIException;
import lib.ApiClient;
import models.api.requests.ExtractorOrderRequest;
import models.api.responses.EmptyResponse;
import models.api.responses.system.ExtractorSummaryResponse;
import models.api.responses.system.ExtractorsResponse;
import play.mvc.Http;

import java.io.IOException;
import java.util.List;
import java.util.SortedMap;

public class ExtractorService {

    private final ApiClient api;
    private final Extractor.Factory extractorFactory;

    @Inject
    private ExtractorService(ApiClient api, Extractor.Factory extractorFactory) {
        this.api = api;
        this.extractorFactory = extractorFactory;
    }

    public void delete(Node node, Input input, String extractorId) throws IOException, APIException {
        api.delete()
                .path("/system/inputs/{0}/extractors/{1}", input.getId(), extractorId)
                .node(node)
                .expect(Http.Status.NO_CONTENT)
                .execute();
    }


    public List<Extractor> all(Node node, Input input) throws IOException, APIException {
        List<Extractor> extractors = Lists.newArrayList();

        final ExtractorsResponse extractorsResponse = api.get(ExtractorsResponse.class)
                .path("/system/inputs/{0}/extractors", input.getId())
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

        api.post(EmptyResponse.class)
                .path("/system/inputs/{0}/extractors/order", inputId)
                .body(req)
                .onlyMasterNode()
                .execute();
    }

}

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
import org.graylog2.restclient.models.api.requests.searches.CreateSavedSearchRequest;
import org.graylog2.restclient.models.api.responses.searches.SavedSearchSummaryResponse;
import org.graylog2.restclient.models.api.responses.searches.SavedSearchesResponse;
import org.graylog2.restroutes.generated.routes;

import java.io.IOException;
import java.util.List;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class SavedSearchService {

    private final ApiClient api;
    private final SavedSearch.Factory savedSearchFactory;

    @Inject
    private SavedSearchService(ApiClient api, SavedSearch.Factory savedSearchFactory) {
        this.api = api;
        this.savedSearchFactory = savedSearchFactory;
    }

    public void create(CreateSavedSearchRequest cssr) throws APIException, IOException {
        api.path(routes.SavedSearchesResource().create())
                .body(cssr)
                .expect(201)
                .execute();
    }

    public List<SavedSearch> all() throws APIException, IOException {
        List<SavedSearch> list = Lists.newArrayList();

        SavedSearchesResponse response = api.path(routes.SavedSearchesResource().list(), SavedSearchesResponse.class).execute();

        for (SavedSearchSummaryResponse search : response.searches) {
            list.add(savedSearchFactory.fromSummaryResponse(search));
        }

        return list;
    }

    public SavedSearch get(String searchId) throws APIException, IOException {
        SavedSearchSummaryResponse response = api.path(routes.SavedSearchesResource().get(searchId), SavedSearchSummaryResponse.class)
                .execute();

        return savedSearchFactory.fromSummaryResponse(response);
    }

    public void delete(SavedSearch search) throws APIException, IOException {
        api.path(routes.SavedSearchesResource().delete(search.getId())).expect(204).execute();
    }
}

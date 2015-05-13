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
import org.graylog2.restclient.models.api.requests.searches.CreateSavedSearchRequest;
import org.graylog2.restclient.models.api.responses.searches.SavedSearchSummaryResponse;
import org.graylog2.restclient.models.api.responses.searches.SavedSearchesResponse;
import org.graylog2.restroutes.generated.routes;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;

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

    public void update(String searchId, CreateSavedSearchRequest cssr) throws APIException, IOException {
        api.path(routes.SavedSearchesResource().update(searchId))
                .body(cssr)
                .expect(200)
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

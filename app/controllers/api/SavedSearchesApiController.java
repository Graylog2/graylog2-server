/**
 * Copyright 2012-2015 TORCH GmbH, 2015 Graylog, Inc.
 *
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
 *
 */
package controllers.api;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import controllers.AuthenticatedController;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.models.SavedSearch;
import org.graylog2.restclient.models.SavedSearchService;
import org.graylog2.restclient.models.api.requests.searches.CreateSavedSearchRequest;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Result;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class SavedSearchesApiController extends AuthenticatedController {
    private final SavedSearchService savedSearchService;

    @Inject
    public SavedSearchesApiController(SavedSearchService savedSearchService) {
        this.savedSearchService = savedSearchService;
    }

    public Result list() {
        try {
            List<Map<String, Object>> response = Lists.newArrayList();

            for (SavedSearch s : savedSearchService.all()) {
                Map<String, Object> search = Maps.newHashMap();
                search.put("id", s.getId());
                search.put("title", s.getTitle());

                response.add(search);
            }

            return ok(Json.toJson(response));
        } catch (IOException e) {
            return internalServerError("io exception");
        } catch (APIException e) {
            return internalServerError("api exception " + e);
        }
    }

    @BodyParser.Of(BodyParser.Json.class)
    public Result create() {
        CreateSavedSearchRequest request = Json.fromJson(request().body().asJson(), CreateSavedSearchRequest.class);

        try {
            savedSearchService.create(request);
        } catch (IOException e) {
            return internalServerError("io exception");
        } catch (APIException e) {
            return internalServerError("api exception " + e);
        }

        return status(202);
    }

    @BodyParser.Of(BodyParser.Json.class)
    public Result update(String searchId) {
        CreateSavedSearchRequest request = Json.fromJson(request().body().asJson(), CreateSavedSearchRequest.class);

        try {
            savedSearchService.update(searchId, request);
            return ok();
        } catch (IOException e) {
            return internalServerError("io exception");
        } catch (APIException e) {
            return internalServerError("api exception " + e);
        }
    }

}

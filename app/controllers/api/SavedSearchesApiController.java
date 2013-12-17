/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
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
 *
 */
package controllers.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.inject.Inject;
import controllers.*;
import controllers.routes;
import lib.APIException;
import lib.ServerNodes;
import models.SavedSearch;
import models.SavedSearchService;
import models.api.requests.searches.CreateSavedSearchRequest;
import play.mvc.Result;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class SavedSearchesApiController extends AuthenticatedController {

    @Inject
    private ServerNodes serverNodes;

    @Inject
    private SavedSearchService savedSearchService;

    public Result list() {
        try {
            List<Map<String, Object>> response = Lists.newArrayList();

            for (SavedSearch s : savedSearchService.all()) {
                Map<String, Object> search = Maps.newHashMap();
                search.put("id", s.getId());
                search.put("title", s.getTitle());

                response.add(search);
            }

            return ok(new Gson().toJson(response)).as("application/json");
        } catch (IOException e) {
            return internalServerError("io exception");
        } catch (APIException e) {
            return internalServerError("api exception " + e);
        }
    }

    public Result execute(String searchId) {
        try {
            SavedSearch search = savedSearchService.get(searchId);

            int relative = 0;
            if (search.getQuery().containsKey("relative")) {
                relative = Integer.parseInt((String) search.getQuery().get("relative"));
            }

            String from = "";
            if (search.getQuery().containsKey("from")) {
                from = (String) search.getQuery().get("from");
            }

            String to = "";
            if (search.getQuery().containsKey("to")) {
                to = (String) search.getQuery().get("to");
            }

            String keyword = "";
            if (search.getQuery().containsKey("keyword")) {
                keyword = (String) search.getQuery().get("keyword");
            }

            return redirect(routes.SearchController.index(
                    (String) search.getQuery().get("query"),
                    (String) search.getQuery().get("rangeType"),
                    relative,
                    from,
                    to,
                    keyword,
                    "minute",
                    0
            ));
        } catch (IOException e) {
            return internalServerError("io exception");
        } catch (APIException e) {
            return internalServerError("api exception " + e);
        }
    }

    public Result create() {
        Map<String,String> params = flattenFormUrlEncoded(request().body().asFormUrlEncoded());
        String json = params.get("params");

        ObjectMapper mapper = new ObjectMapper();
        CreateSavedSearchRequest request;
        try {
            request = mapper.readValue(json, CreateSavedSearchRequest.class);
            request.creatorUserId = currentUser().getName();
        } catch (IOException e) {
            return internalServerError("Could not parse JSON into request.");
        }

        try {
            savedSearchService.create(request);
        } catch (IOException e) {
            return internalServerError("io exception");
        } catch (APIException e) {
            return internalServerError("api exception " + e);
        }

        return status(202);
    }

}

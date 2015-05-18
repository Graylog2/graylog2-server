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
package controllers;

import com.google.inject.Inject;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.lib.ApiClient;
import org.graylog2.restclient.models.SavedSearch;
import org.graylog2.restclient.models.SavedSearchService;
import play.api.mvc.Call;
import play.mvc.Result;

import java.io.IOException;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class SavedSearchesController extends AuthenticatedController {

    @Inject
    private SavedSearchService savedSearchService;

    public Result execute(String searchId, String streamId, int displayWidth) {
        try {
            SavedSearch search = savedSearchService.get(searchId);

            if(streamId == null || streamId.isEmpty()) {
                return redirect(callFromSavedSearch(search, streamId, true, displayWidth));
            } else {
                return redirect(callFromSavedSearch(search, streamId, true, displayWidth));
            }

        } catch (IOException e) {
            return status(500, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        } catch (APIException e) {
            String message = "Could not fetch saved search information. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(500, views.html.errors.error.render(message, e, request()));
        }
    }

    public Result delete(String searchId) {
        try {
            SavedSearch search = savedSearchService.get(searchId);

            savedSearchService.delete(search);
            flash("success", "Saved search deleted.");

            return redirect("/");
        } catch (IOException e) {
            return status(500, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        } catch (APIException e) {
            String message = "Could not fetch saved search information. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(500, views.html.errors.error.render(message, e, request()));
        }
    }

    private Call callFromSavedSearch(SavedSearch search, String streamId, boolean includeOriginal, int displayWidth) {
        Map<String, Object> searchQuery = search.getQuery();

        int relative = 0;
        if (searchQuery.containsKey("relative")) {
            final Object persistedRelative = searchQuery.get("relative");

            relative = persistedRelative instanceof String ? Integer.parseInt(String.valueOf(persistedRelative)) : (Integer) persistedRelative;
        }

        String from = "";
        if (searchQuery.containsKey("from")) {
            from = (String) searchQuery.get("from");
        }

        String to = "";
        if (searchQuery.containsKey("to")) {
            to = (String) searchQuery.get("to");
        }

        String keyword = "";
        if (searchQuery.containsKey("keyword")) {
            keyword = (String) searchQuery.get("keyword");
        }

        String fields = "";
        if (searchQuery.containsKey("fields")) {
            fields = (String) searchQuery.get("fields");
        }

        String interval = "";
        if (searchQuery.containsKey("interval")) {
            interval = (String) searchQuery.get("interval");
        }

        String searchId = "";
        if (includeOriginal) {
            searchId = search.getId();
        }

        if (streamId == null || streamId.isEmpty()) {
            return routes.SearchController.index(
                    (String) searchQuery.get("query"),
                    (String) searchQuery.get("rangeType"),
                    relative,
                    from,
                    to,
                    keyword,
                    interval,
                    0,
                    searchId,
                    "",
                    "",
                    fields,
                    displayWidth
            );
        } else {
            return routes.StreamSearchController.index(
                    streamId,
                    (String) searchQuery.get("query"),
                    (String) searchQuery.get("rangeType"),
                    relative,
                    from,
                    to,
                    keyword,
                    interval,
                    0,
                    searchId,
                    "",
                    "",
                    fields,
                    displayWidth
            );
        }
    }
}

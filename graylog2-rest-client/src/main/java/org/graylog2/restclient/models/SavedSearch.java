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

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.restclient.models.api.responses.searches.SavedSearchSummaryResponse;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.Map;

public class SavedSearch {

    public interface Factory {
        public SavedSearch fromSummaryResponse(SavedSearchSummaryResponse sssr);
    }

    private String id;
    private String title;
    private Map<String, Object> query;
    private DateTime createdAt;
    private User creatorUserId;

    @AssistedInject
    private SavedSearch(UserService userService, @Assisted SavedSearchSummaryResponse ssr) {
        this.id = ssr.id;
        this.title = ssr.title;
        this.query = ssr.query;
        this.createdAt = new DateTime(ssr.createdAt, DateTimeZone.UTC);
        this.creatorUserId = userService.load(ssr.creatorUserId);
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public Map<String, Object> getQuery() {
        return query;
    }

    public DateTime getCreatedAt() {
        return createdAt;
    }

    public User getCreatorUserId() {
        return creatorUserId;
    }

}

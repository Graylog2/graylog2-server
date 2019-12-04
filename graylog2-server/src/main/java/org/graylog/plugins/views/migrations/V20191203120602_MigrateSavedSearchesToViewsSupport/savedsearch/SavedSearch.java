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
package org.graylog.plugins.views.migrations.V20191203120602_MigrateSavedSearchesToViewsSupport.savedsearch;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.joda.time.DateTime;

@AutoValue
@JsonAutoDetect
public abstract class SavedSearch {
    public abstract String id();
    public abstract String title();
    public abstract Query query();
    public abstract DateTime createdAt();
    public abstract String creatorUserId();

    @JsonCreator
    static SavedSearch create(
            @JsonProperty("_id") String id,
            @JsonProperty("title") String title,
            @JsonProperty("query") Query query,
            @JsonProperty("created_at") DateTime createdAt,
            @JsonProperty("creator_user_id") String creatorUserId
    ) {
        return new AutoValue_SavedSearch(id, title, query, createdAt, creatorUserId);
    }
}

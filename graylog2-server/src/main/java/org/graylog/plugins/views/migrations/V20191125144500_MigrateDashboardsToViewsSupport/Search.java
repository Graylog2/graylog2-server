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
package org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.joda.time.DateTime;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@AutoValue
@JsonAutoDetect
abstract class Search {
    static final String FIELD_REQUIRES = "requires";
    private static final String FIELD_CREATED_AT = "created_at";
    static final String FIELD_OWNER = "owner";

    @Id
    @ObjectId
    @Nullable
    @JsonProperty
    abstract String id();

    @JsonProperty
    abstract Set<Query> queries();

    @JsonProperty
    Set<Object> parameters() {
        return Collections.emptySet();
    }

    @JsonProperty(FIELD_REQUIRES)
    Map<String, Object> requires() {
        return Collections.emptyMap();
    }

    @JsonProperty(FIELD_OWNER)
    abstract Optional<String> owner();

    @JsonProperty(FIELD_CREATED_AT)
    abstract DateTime createdAt();

    private static String newId() {
        return new org.bson.types.ObjectId().toHexString();
    }

    static Search create(
            Set<Query> queries,
            String owner,
            DateTime createdAt
    ) {
        return new AutoValue_Search(newId(), queries, Optional.ofNullable(owner), createdAt);
    }
}

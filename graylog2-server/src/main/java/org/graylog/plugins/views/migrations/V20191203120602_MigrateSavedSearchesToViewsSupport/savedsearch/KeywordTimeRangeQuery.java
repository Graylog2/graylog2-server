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
import org.graylog.plugins.views.migrations.V20191203120602_MigrateSavedSearchesToViewsSupport.view.KeywordRange;
import org.graylog.plugins.views.migrations.V20191203120602_MigrateSavedSearchesToViewsSupport.view.TimeRange;

import javax.annotation.Nullable;
import java.util.Optional;

@AutoValue
@JsonAutoDetect
public abstract class KeywordTimeRangeQuery implements Query {
    public static final String type = "keyword";

    public abstract String rangeType();
    public abstract String fields();
    public abstract String query();

    public abstract String keyword();

    @Override
    public TimeRange toTimeRange() {
        return KeywordRange.create(keyword());
    }

    @JsonCreator
    static KeywordTimeRangeQuery create(
            @JsonProperty("rangeType") String rangeType,
            @JsonProperty("fields") String fields,
            @JsonProperty("query") String query,
            @JsonProperty("keyword") String keyword,
            @JsonProperty("streamId") @Nullable String streamId
    ) {
        return new AutoValue_KeywordTimeRangeQuery(Optional.ofNullable(streamId), rangeType, fields, query, keyword);
    }
}

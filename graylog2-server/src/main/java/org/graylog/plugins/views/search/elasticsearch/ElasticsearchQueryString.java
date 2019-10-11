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
package org.graylog.plugins.views.search.elasticsearch;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.views.search.engine.BackendQuery;

@AutoValue
@JsonTypeName(ElasticsearchQueryString.NAME)
@JsonDeserialize(builder = AutoValue_ElasticsearchQueryString.Builder.class)
public abstract class ElasticsearchQueryString implements BackendQuery {

    public static final String NAME = "elasticsearch";

    @Override
    public abstract String type();

    @JsonProperty
    public abstract String queryString();

    public static Builder builder() {
        return new AutoValue_ElasticsearchQueryString.Builder().type(NAME);
    }

    @Override
    public String toString() {
        return type() + ": " + queryString();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonProperty
        public abstract Builder type(String type);

        @JsonProperty
        public abstract Builder queryString(String queryString);

        public abstract ElasticsearchQueryString build();
    }
}

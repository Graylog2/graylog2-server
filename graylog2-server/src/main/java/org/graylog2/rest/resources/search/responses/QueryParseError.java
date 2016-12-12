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
package org.graylog2.rest.resources.search.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

import javax.annotation.Nullable;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class QueryParseError {
    @JsonProperty
    public abstract String query();

    @JsonProperty
    @Nullable
    public abstract Integer beginColumn();

    @JsonProperty
    @Nullable
    public abstract Integer beginLine();

    @JsonProperty
    @Nullable
    public abstract Integer endColumn();

    @JsonProperty
    @Nullable
    public abstract Integer endLine();

    @JsonProperty
    @Nullable
    public abstract String message();

    @JsonProperty
    public abstract String exceptionName();

    public static QueryParseError create(String query,
                                         @Nullable Integer beginColumn,
                                         @Nullable Integer beginLine,
                                         @Nullable Integer endColumn,
                                         @Nullable Integer endLine,
                                         @Nullable String message,
                                         String exceptionName) {
        return new AutoValue_QueryParseError(query, beginColumn, beginLine, endColumn, endLine, message, exceptionName);
    }

    public static QueryParseError create(String query, @Nullable String message, String exceptionName) {
        return create(query, null, null, null, null, message, exceptionName);
    }
}

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
package org.graylog2.rest.resources.search.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

@JsonAutoDetect
@AutoValue
public abstract class QueryParseError {
    @JsonProperty
    public abstract String query();

    @JsonProperty
    public abstract int beginColumn();

    @JsonProperty
    public abstract int beginLine();

    @JsonProperty
    public abstract int endColumn();

    @JsonProperty
    public abstract int endLine();

    public static QueryParseError create(String query, int beginColumn, int beginLine, int endColumn, int endLine) {
        return new AutoValue_QueryParseError(query, beginColumn, beginLine, endColumn, endLine);
    }

    public static QueryParseError create(String query) {
        return new AutoValue_QueryParseError(query, 0, 0, 0, 0);
    }
}

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
package org.graylog2.restclient.models.api.responses;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.Nullable;

public class QueryParseError {
    @JsonProperty("query")
    public String query;

    @JsonProperty("message")
    @Nullable
    public String message;

    @JsonProperty("begin_column")
    @Nullable
    public Integer beginColumn;

    @JsonProperty("begin_line")
    @Nullable
    public Integer beginLine;

    @JsonProperty("end_column")
    @Nullable
    public Integer endColumn;

    @JsonProperty("end_line")
    @Nullable
    public Integer endLine;

    @JsonProperty("exception_name")
    public String exceptionName;
}
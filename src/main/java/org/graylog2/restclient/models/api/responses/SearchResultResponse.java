/*
 * Copyright 2013 TORCH UG
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
 */
package org.graylog2.restclient.models.api.responses;

import com.google.gson.annotations.SerializedName;
import org.joda.time.DateTime;

import java.util.List;

public class SearchResultResponse {
	
	public int time;
	public String query;
	public long total_results;
	public List<MessageSummaryResponse> messages;
	public List<String> fields;

    @SerializedName("used_indices")
    public List<String> usedIndices;

    @SerializedName("built_query")
    public String builtQuery;

    public ParseError error;

    @SerializedName("generic_error")
    public GenericError genericError;

    public String from;

    public String to;

    public DateTime getFromDataTime() {
        return DateTime.parse(from);
    }
    public DateTime getToDataTime() {
        return DateTime.parse(to);
    }

    public abstract static class QueryError {}

    public static class ParseError extends QueryError {
        @SerializedName("begin_column")
        public int beginColumn;

        @SerializedName("begin_line")
        public int beginLine;

        @SerializedName("end_column")
        public int endColumn;

        @SerializedName("end_line")
        public int endLine;
    }

    public static class GenericError extends QueryError {
        @SerializedName("exception_name")
        public String exceptionName;

        public String message;
    }

}

/**
 * The MIT License
 * Copyright (c) 2012 TORCH GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.graylog2.restclient.models.api.responses;

import org.graylog2.restclient.models.api.responses.system.indices.IndexRangeSummary;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;

import java.util.List;

public class SearchResultResponse {

    public int time;
    public String query;
    public long total_results;
    public List<MessageSummaryResponse> messages;
    public List<String> fields;

    @JsonProperty("used_indices")
    public List<IndexRangeSummary> usedIndices;

    @JsonProperty("built_query")
    public String builtQuery;

    public ParseError error;

    @JsonProperty("generic_error")
    public GenericError genericError;

    public String from;

    public String to;

    public DateTime getFromDataTime() {
        return from != null ? DateTime.parse(from) : null;
    }
    public DateTime getToDataTime() {
        return to != null ? DateTime.parse(to) : null;
    }

    public abstract static class QueryError {}

    public static class ParseError extends QueryError {
        @JsonProperty("begin_column")
        public int beginColumn;

        @JsonProperty("begin_line")
        public int beginLine;

        @JsonProperty("end_column")
        public int endColumn;

        @JsonProperty("end_line")
        public int endLine;
    }

    public static class GenericError extends QueryError {
        @JsonProperty("exception_name")
        public String exceptionName;

        public String message;
    }

}

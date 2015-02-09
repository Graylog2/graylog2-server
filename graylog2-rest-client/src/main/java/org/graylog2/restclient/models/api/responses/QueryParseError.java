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
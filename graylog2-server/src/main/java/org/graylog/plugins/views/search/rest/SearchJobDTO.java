package org.graylog.plugins.views.search.rest;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.views.search.QueryResult;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.errors.SearchError;

import java.util.Map;
import java.util.Set;

@AutoValue
@JsonAutoDetect
@JsonPropertyOrder({"execution", "results"})
abstract class SearchJobDTO {
    @JsonProperty
    abstract String id();

    @JsonProperty("search_id")
    abstract String searchId();

    @JsonProperty
    abstract String owner();

    @JsonProperty("errors")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    abstract Set<SearchError> errors();

    @JsonProperty
    abstract Map<String, QueryResult> results();

    static SearchJobDTO fromSearchJob(SearchJob searchJob) {
        return Builder.create()
                .id(searchJob.getId())
                .owner(searchJob.getOwner())
                .errors(searchJob.getErrors())
                .results(searchJob.results())
                .searchId(searchJob.getSearchId())
                .build();
    }

    @AutoValue.Builder
    abstract static class Builder {
        abstract Builder id(String id);

        abstract Builder searchId(String searchId);

        abstract Builder owner(String owner);

        abstract Builder errors(Set<SearchError> errors);

        abstract Builder results(Map<String, QueryResult> results);

        abstract SearchJobDTO build();

        static SearchJobDTO.Builder create() {
            return new AutoValue_SearchJobDTO.Builder();
        }
    }
}

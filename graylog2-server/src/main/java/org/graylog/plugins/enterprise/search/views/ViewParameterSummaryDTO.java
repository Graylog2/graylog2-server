package org.graylog.plugins.enterprise.search.views;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.enterprise.search.Parameter;
import org.graylog.plugins.enterprise.search.Search;

import java.util.Collection;

@AutoValue
@JsonAutoDetect
public abstract class ViewParameterSummaryDTO {
    public static final String FIELD_ID = "id";
    public static final String FIELD_TITLE = "title";
    public static final String FIELD_SUMMARY = "summary";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_PARAMETERS = "parameters";

    @JsonProperty(FIELD_ID)
    public abstract String id();

    @JsonProperty(FIELD_TITLE)
    public abstract String title();

    @JsonProperty(FIELD_SUMMARY)
    public abstract String summary();

    @JsonProperty(FIELD_DESCRIPTION)
    public abstract String description();

    @JsonProperty(FIELD_PARAMETERS)
    public abstract Collection<Parameter> parameters();

    static ViewParameterSummaryDTO create(ViewDTO view, Search search) {
        return new AutoValue_ViewParameterSummaryDTO(view.id(), view.title(), view.summary(), view.description(), search.parameters());
    }
}

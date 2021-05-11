package org.graylog.plugins.views.search.export;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import javax.validation.constraints.NotEmpty;

@AutoValue
@JsonAutoDetect
public abstract class SearchExportJob implements ExportJob {
    static final String TYPE = "search_export";
    private static final String FIELD_SEARCH_ID = "search_id";
    private static final String FIELD_RESULT_FORMAT = "result_format";

    @JsonProperty("type")
    public String type() {
        return TYPE;
    }

    @JsonProperty(FIELD_SEARCH_ID)
    public abstract String searchId();

    @JsonProperty(FIELD_RESULT_FORMAT)
    @NotEmpty
    public abstract ResultFormat resultFormat();

    static SearchExportJob forSearch(String id, String searchId, ResultFormat resultFormat) {
        return new AutoValue_SearchExportJob(id, DateTime.now(DateTimeZone.UTC), searchId, resultFormat);
    }

    @JsonCreator
    static SearchExportJob create(
            @JsonProperty(FIELD_ID) String id,
            @JsonProperty(FIELD_SEARCH_ID) String searchId,
            @JsonProperty(FIELD_RESULT_FORMAT) ResultFormat resultFormat
    ) {
        return new AutoValue_SearchExportJob(id,
                DateTime.now(DateTimeZone.UTC),
                searchId,
                resultFormat);
    }
}

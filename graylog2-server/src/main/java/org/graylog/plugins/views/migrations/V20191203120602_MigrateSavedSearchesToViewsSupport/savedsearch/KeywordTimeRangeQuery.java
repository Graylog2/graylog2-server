package org.graylog.plugins.views.migrations.V20191203120602_MigrateSavedSearchesToViewsSupport.savedsearch;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.views.migrations.V20191203120602_MigrateSavedSearchesToViewsSupport.view.KeywordRange;
import org.graylog.plugins.views.migrations.V20191203120602_MigrateSavedSearchesToViewsSupport.view.TimeRange;

@AutoValue
@JsonAutoDetect
public abstract class KeywordTimeRangeQuery implements Query {
    public static final String type = "keyword";

    public abstract String rangeType();
    public abstract String fields();
    public abstract String query();

    public abstract String keyword();

    @Override
    public TimeRange toTimeRange() {
        return KeywordRange.create(keyword());
    }

    @JsonCreator
    static KeywordTimeRangeQuery create(
            @JsonProperty("rangeType") String rangeType,
            @JsonProperty("fields") String fields,
            @JsonProperty("query") String query,
            @JsonProperty("keyword") String keyword
    ) {
        return new AutoValue_KeywordTimeRangeQuery(rangeType, fields, query, keyword);
    }
}

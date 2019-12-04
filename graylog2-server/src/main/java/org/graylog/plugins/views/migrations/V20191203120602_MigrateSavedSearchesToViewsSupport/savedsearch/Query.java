package org.graylog.plugins.views.migrations.V20191203120602_MigrateSavedSearchesToViewsSupport.savedsearch;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.base.Splitter;
import org.graylog.plugins.views.migrations.V20191203120602_MigrateSavedSearchesToViewsSupport.view.TimeRange;

import java.util.Collections;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "rangeType", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = AbsoluteTimeRangeQuery.class, name = AbsoluteTimeRangeQuery.type),
        @JsonSubTypes.Type(value = KeywordTimeRangeQuery.class, name = KeywordTimeRangeQuery.type),
        @JsonSubTypes.Type(value = RelativeTimeRangeQuery.class, name = RelativeTimeRangeQuery.type)
})
public interface Query {
    String rangeType();
    String fields();
    String query();

    TimeRange toTimeRange();

    default List<String> fieldsList() {
        return isNullOrEmpty(fields())
                ? Collections.emptyList()
                : Splitter.on(",").splitToList(fields());
    }
}

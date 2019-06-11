package org.graylog.plugins.enterprise.search.searchtypes;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.elasticsearch.search.sort.SortOrder;

@AutoValue
public abstract class Sort {

    @JsonProperty
    public abstract String field();

    @JsonProperty
    public abstract SortOrder order();

    @JsonCreator
    public static Sort create(@JsonProperty("field") String field, @JsonProperty("order") SortOrder order) {
        return new AutoValue_Sort(field, order);
    }

}

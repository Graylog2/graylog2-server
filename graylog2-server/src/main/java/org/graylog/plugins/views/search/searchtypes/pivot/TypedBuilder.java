package org.graylog.plugins.views.search.searchtypes.pivot;

import com.fasterxml.jackson.annotation.JsonProperty;

public abstract class TypedBuilder<V, B> {
    @JsonProperty
    public abstract B type(String type);

    public abstract V build();
}

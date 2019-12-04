package org.graylog.plugins.views.migrations.V20191203120602_MigrateSavedSearchesToViewsSupport.search;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.joda.time.DateTime;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@AutoValue
@JsonAutoDetect
public abstract class Search {
    static final String FIELD_REQUIRES = "requires";
    private static final String FIELD_CREATED_AT = "created_at";
    static final String FIELD_OWNER = "owner";

    @Id
    @ObjectId
    @Nullable
    @JsonProperty
    public abstract String id();

    @JsonProperty
    abstract Set<Query> queries();

    @JsonProperty
    Set<Object> parameters() {
        return Collections.emptySet();
    }

    @JsonProperty(FIELD_REQUIRES)
    Map<String, Object> requires() {
        return Collections.emptyMap();
    }

    @JsonProperty(FIELD_OWNER)
    abstract Optional<String> owner();

    @JsonProperty(FIELD_CREATED_AT)
    abstract DateTime createdAt();

    public static Search create(
            String id,
            Set<Query> queries,
            String owner,
            DateTime createdAt
    ) {
        return new AutoValue_Search(id, queries, Optional.ofNullable(owner), createdAt);
    }
}

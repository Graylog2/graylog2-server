package org.graylog.plugins.views.migrations.V20191203120602_MigrateSavedSearchesToViewsSupport.savedsearch;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.joda.time.DateTime;

@AutoValue
@JsonAutoDetect
public abstract class SavedSearch {
    public abstract String id();
    public abstract String title();
    public abstract Query query();
    public abstract DateTime createdAt();
    public abstract String creatorUserId();

    @JsonCreator
    static SavedSearch create(
            @JsonProperty("_id") String id,
            @JsonProperty("title") String title,
            @JsonProperty("query") Query query,
            @JsonProperty("created_at") DateTime createdAt,
            @JsonProperty("creator_user_id") String creatorUserId
    ) {
        return new AutoValue_SavedSearch(id, title, query, createdAt, creatorUserId);
    }
}

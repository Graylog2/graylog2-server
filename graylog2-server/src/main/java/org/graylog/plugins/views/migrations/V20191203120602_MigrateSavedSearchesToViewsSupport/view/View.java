package org.graylog.plugins.views.migrations.V20191203120602_MigrateSavedSearchesToViewsSupport.view;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.joda.time.DateTime;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;
import javax.validation.constraints.NotBlank;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@AutoValue
@JsonAutoDetect
public abstract class View {
    private static final String FIELD_ID = "id";
    private static final String FIELD_TYPE = "type";
    private static final String FIELD_TITLE = "title";
    private static final String FIELD_SUMMARY = "summary";
    private static final String FIELD_DESCRIPTION = "description";
    private static final String FIELD_SEARCH_ID = "search_id";
    public static final String FIELD_CONTENT_PACK = "content_pack";
    private static final String FIELD_PROPERTIES = "properties";
    private static final String FIELD_REQUIRES = "requires";
    private static final String FIELD_STATE = "state";
    private static final String FIELD_CREATED_AT = "created_at";
    private static final String FIELD_OWNER = "owner";

    @ObjectId
    @Id
    @Nullable
    @JsonProperty(FIELD_ID)
    public abstract String id();

    @JsonProperty(FIELD_TYPE)
    String type() {
        return "SEARCH";
    }

    @JsonProperty(FIELD_TITLE)
    @NotBlank
    abstract String title();

    @JsonProperty(FIELD_SUMMARY)
    abstract String summary();

    @JsonProperty(FIELD_DESCRIPTION)
    abstract String description();

    @JsonProperty(FIELD_SEARCH_ID)
    abstract String searchId();

    @JsonProperty(FIELD_CONTENT_PACK)
    Optional<String> contentPack() {
        return Optional.empty();
    }

    @JsonProperty(FIELD_PROPERTIES)
    Set<String> properties() {
        return Collections.emptySet();
    }

    @JsonProperty(FIELD_REQUIRES)
    Map<String, Object> requires() {
        return Collections.emptyMap();
    }

    @JsonProperty(FIELD_STATE)
    abstract Map<String, ViewState> state();

    @JsonProperty(FIELD_OWNER)
    abstract Optional<String> owner();

    @JsonProperty(FIELD_CREATED_AT)
    abstract DateTime createdAt();

    public static View create(String id,
                       String title,
                       String summary,
                       String description,
                       String searchId,
                       Map<String, ViewState> state,
                       Optional<String> owner,
                       DateTime createdAt) {
        return new AutoValue_View(id, title, summary, description, searchId, state, owner, createdAt);
    }
}

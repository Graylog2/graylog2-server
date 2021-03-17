package org.graylog.plugins.views.search.views;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.joda.time.DateTime;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;
import javax.validation.constraints.NotBlank;
import java.util.Optional;

@JsonSerialize(as=SummaryViewDTO.class)
public interface SummaryViewDTO {
    @ObjectId
    @Id
    @Nullable
    @JsonProperty(ViewDTO.FIELD_ID)
    String id();

    @JsonProperty(ViewDTO.FIELD_TYPE)
    ViewDTO.Type type();

    @JsonProperty(ViewDTO.FIELD_TITLE)
    @NotBlank
    String title();

    // A short, one sentence description of the view
    @JsonProperty(ViewDTO.FIELD_SUMMARY)
    String summary();

    // A longer description of the view, probably including markup text
    @JsonProperty(ViewDTO.FIELD_DESCRIPTION)
    String description();

    @JsonProperty(ViewDTO.FIELD_SEARCH_ID)
    String searchId();

    @JsonProperty(ViewDTO.FIELD_OWNER)
    Optional<String> owner();

    @JsonProperty(ViewDTO.FIELD_CREATED_AT)
    DateTime createdAt();
}

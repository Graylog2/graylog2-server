package org.graylog2.contentpacks.model.entities;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import jakarta.validation.constraints.NotBlank;
import org.graylog.plugins.views.search.searchfilters.model.UsedSearchFilter;
import org.graylog2.contentpacks.exceptions.ContentPackException;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.references.ValueReference;

import java.util.Map;

@JsonAutoDetect
@AutoValue
@JsonDeserialize(builder = SearchFilterEntity.Builder.class)
public abstract class SearchFilterEntity extends ScopedContentPackEntity {
    private static final String SEARCH_FILTER_FIELD = "search_filter";

    @NotBlank
    @JsonProperty(UsedSearchFilter.ID_FIELD)
    public abstract ValueReference id();

    @NotBlank
    @JsonProperty(UsedSearchFilter.TITLE_FIELD)
    public abstract ValueReference title();

    @NotBlank
    @JsonProperty(SEARCH_FILTER_FIELD)
    public abstract UsedSearchFilter searchFilter();

    public static Builder builder() {
        return Builder.create();
    }

    @AutoValue.Builder
    public abstract static class Builder extends ScopedContentPackEntity.AbstractBuilder<Builder> {

        @JsonCreator
        public static Builder create() {
            return new AutoValue_SearchFilterEntity.Builder();
        }

        @JsonProperty(UsedSearchFilter.ID_FIELD)
        public abstract Builder id(ValueReference id);

        @JsonProperty(UsedSearchFilter.TITLE_FIELD)
        public abstract Builder title(ValueReference title);

        @JsonProperty(SEARCH_FILTER_FIELD)
        public abstract Builder searchFilter(UsedSearchFilter searchFilter);

        public abstract SearchFilterEntity build();
    }

    public UsedSearchFilter toNativeEntity(Map<String, ValueReference> parameters, Map<EntityDescriptor, Object> natvieEntities) {
        final String filterId = id().asString(parameters);
        final EntityDescriptor filterDescriptor = EntityDescriptor.create(filterId, ModelTypes.SEARCH_FILTER_V1);
        final Object filter = natvieEntities.get(filterDescriptor);

        if (filter instanceof UsedSearchFilter) {
            return (UsedSearchFilter) filter;
        } else {
            throw new ContentPackException("Invalid type for search filter (" + filterId + ")");
        }
    }
}

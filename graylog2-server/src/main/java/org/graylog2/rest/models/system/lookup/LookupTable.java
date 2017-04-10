package org.graylog2.rest.models.system.lookup;

import com.google.auto.value.AutoValue;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import org.graylog.autovalue.WithBeanGetter;

import javax.annotation.Nullable;

@AutoValue
@JsonAutoDetect
@WithBeanGetter
@JsonDeserialize(builder = LookupTable.Builder.class)
public abstract class LookupTable {

    @Nullable
    @JsonProperty("id")
    public abstract String id();

    @JsonProperty("title")
    public abstract String title();

    @JsonProperty("description")
    public abstract String description();

    @JsonProperty("name")
    public abstract String name();

    @JsonProperty("cache_name")
    public abstract String cacheProviderName();

    @JsonProperty("data_provider_name")
    public abstract String dataProviderName();


    public static LookupTable fromDomainObject(org.graylog2.lookup.LookupTable lookupTable) {
        return LookupTable.builder()
                .id(lookupTable.id())
                .title(lookupTable.title())
                .description(lookupTable.description())
                .name(lookupTable.name())
                .cacheProviderName(lookupTable.cacheProvider().id())
                .dataProviderName(lookupTable.dataProvider().id())
                .build();
    }

    public static Builder builder() {
        return new AutoValue_LookupTable.Builder();
    }

    public org.graylog2.lookup.LookupTable.Builder toDomainObjectBuilder() {
        return org.graylog2.lookup.LookupTable.builder()
                .id(id())
                .title(title())
                .description(description())
                .name(name());
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder id(String id);

        public abstract Builder title(String title);

        public abstract Builder description(String description);

        public abstract Builder name(String name);

        public abstract Builder cacheProviderName(String cacheProviderName);

        public abstract Builder dataProviderName(String dataProviderName);

        public abstract LookupTable build();
    }
}

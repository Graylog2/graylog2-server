package org.graylog2.lookup.dto;

import com.google.auto.value.AutoValue;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.lookup.LookupTable;
import org.mongojack.Id;
import org.mongojack.MongoCollection;
import org.mongojack.ObjectId;

import java.util.Map;

import javax.annotation.Nullable;

@AutoValue
@WithBeanGetter
@JsonDeserialize(builder = LookupTableDto.Builder.class)
@MongoCollection(name = "lut_tables")
public abstract class LookupTableDto {

    @Id
    @ObjectId
    @Nullable
    @JsonProperty("id")
    public abstract String id();

    @JsonProperty("title")
    public abstract String title();

    @JsonProperty("description")
    public abstract String description();

    @JsonProperty("name")
    public abstract String name();

    @ObjectId
    @JsonProperty("cache")
    public abstract String cacheId();

    @ObjectId
    @JsonProperty("data_provider")
    public abstract String dataProviderId();

    public static Builder builder() {
        return new AutoValue_LookupTableDto.Builder();
    }

    public static LookupTableDto fromDomainObject(LookupTable table) {
        return builder()
                .id(table.id())
                .title(table.title())
                .description(table.description())
                .name(table.name())
                .cacheId(table.cacheProvider().id())
                .dataProviderId(table.dataProvider().id())
                .build();
    }

    public static LookupTable toDomainObject(LookupTableDto savedObject) {
        return LookupTable.builder()
                .id(savedObject.id())
                .title(savedObject.title())
                .description(savedObject.description())
                .name(savedObject.name())
                .build();
    }

    public LookupTable toDomainObject(Map<String, CacheConfigurationDto> cacheMap, Map<String, DataProviderDto> dataProviderMap) {
        return LookupTable.builder()
                .id(id())
                .title(title())
                .description(description())
                .name(name())
                .cacheProvider(CacheConfigurationDto.toDomainObject(cacheMap.get(cacheId())))
                .dataProvider(DataProviderDto.toDomainObject(dataProviderMap.get(dataProviderId())))
                .build();
    }


    @AutoValue.Builder
    public abstract static class Builder {
        @JsonProperty("id")
        public abstract Builder id(@Nullable String id);

        @JsonProperty("title")
        public abstract Builder title(String title);

        @JsonProperty("description")
        public abstract Builder description(String description);

        @JsonProperty("name")
        public abstract Builder name(String name);

        @JsonProperty("cache")
        public abstract Builder cacheId(String id);

        @JsonProperty("data_provider")
        public abstract Builder dataProviderId(String id);

        public abstract LookupTableDto build();
    }
}

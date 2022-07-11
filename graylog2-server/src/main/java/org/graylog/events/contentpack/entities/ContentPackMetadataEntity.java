package org.graylog.events.contentpack.entities;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.database.entities.EntityMetadata;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Map;

@AutoValue
@JsonAutoDetect
public abstract class ContentPackMetadataEntity {

    private static final String FIELD_VERSION = "version";
    private static final String FIELD_REVISION = "revision";
    private static final String FIELD_SCOPE = "scope";
    private static final String FIELD_CREATED_AT = "created_at";
    private static final String FIELD_UPDATED_AT = "updated_at";

    @JsonProperty(FIELD_VERSION)
    public abstract ValueReference version();

    @JsonProperty(FIELD_REVISION)
    public abstract ValueReference revision();

    @JsonProperty(FIELD_SCOPE)
    public abstract ValueReference scope();

    @JsonProperty(FIELD_CREATED_AT)
    public abstract ValueReference createdAt();

    @JsonProperty(FIELD_UPDATED_AT)
    public abstract ValueReference updatedAt();

    public EntityMetadata toEntityMetadata() {
        final Map<String, ValueReference> params = Collections.emptyMap();
        final ZonedDateTime creationDate = Instant.ofEpochSecond(createdAt().asLong(params)).atZone(ZoneOffset.UTC);
        final ZonedDateTime updateDate = Instant.ofEpochSecond(updatedAt().asLong(params)).atZone(ZoneOffset.UTC);

        return EntityMetadata.Builder
                .create()
                .version(version().asString())
                .rev(revision().asLong(params))
                .scope(scope().asString())
                .createdAt(creationDate)
                .updatedAt(updateDate)
                .build();
    }

    @JsonCreator
    public static ContentPackMetadataEntity create(@JsonProperty(FIELD_VERSION) ValueReference version,
                                                   @JsonProperty(FIELD_REVISION) ValueReference revision,
                                                   @JsonProperty(FIELD_SCOPE) ValueReference scope,
                                                   @JsonProperty(FIELD_CREATED_AT) ValueReference createdAt,
                                                   @JsonProperty(FIELD_UPDATED_AT) ValueReference updatedAt) {
        return new AutoValue_ContentPackMetadataEntity(version, revision, scope, createdAt, updatedAt);
    }

    public static ContentPackMetadataEntity of(EntityMetadata metadata) {
        return create(ValueReference.of(metadata.version()),
                ValueReference.of(metadata.rev()),
                ValueReference.of(metadata.scope()),
                ValueReference.of(metadata.createdAt().toEpochSecond()),
                ValueReference.of(metadata.updatedAt().toEpochSecond()));
    }


}

package org.graylog2.indexer.migration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.indexer.datanode.RemoteReindexingMigrationAdapter.Status;

import javax.annotation.Nullable;
import java.util.Collection;

@AutoValue
@JsonDeserialize(builder = RemoteReindexMigration.Builder.class)
@WithBeanGetter
public abstract class RemoteReindexMigration {
    private static final String FIELD_STATUS = "status";
    private static final String FIELD_INDICES = "indices";
    private static final String FIELD_ERROR = "error";

    @JsonProperty(FIELD_STATUS)
    public abstract Status status();

    @JsonProperty(FIELD_INDICES)
    public abstract Collection<RemoteReindexIndex> indices();

    @JsonProperty(FIELD_ERROR)
    @Nullable
    public abstract String error();

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonProperty(FIELD_STATUS)
        public abstract Builder status(Status status);

        @JsonProperty(FIELD_STATUS)
        public abstract Builder indices(Collection<RemoteReindexIndex> indices);

        @JsonProperty(FIELD_ERROR)
        @Nullable
        public abstract Builder error(String error);

        @JsonCreator
        public static Builder create() {
            return new AutoValue_RemoteReindexMigration.Builder();
        }

        public abstract RemoteReindexMigration build();
    }
}

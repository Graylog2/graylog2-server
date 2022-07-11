/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.myentity;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog2.database.entities.EntityMetadata;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;
import java.util.Optional;

final class AutoValue_MyEntity extends MyEntity {

    private final String id;

    private final EntityMetadata metadata;

    private final String title;

    private final Optional<String> description;

    private AutoValue_MyEntity(
            @Nullable String id,
            EntityMetadata metadata,
            String title,
            Optional<String> description) {
        this.id = id;
        this.metadata = metadata;
        this.title = title;
        this.description = description;
    }

    @Id
    @ObjectId
    @Nullable
    @JsonProperty("id")
    @Override
    public String id() {
        return id;
    }

    @JsonProperty("_metadata")
    @Override
    public EntityMetadata metadata() {
        return metadata;
    }

    @JsonProperty("title")
    @Override
    public String title() {
        return title;
    }

    @JsonProperty("description")
    @Override
    public Optional<String> description() {
        return description;
    }

    @Override
    public String toString() {
        return "MyEntity{"
                + "id=" + id + ", "
                + "metadata=" + metadata + ", "
                + "title=" + title + ", "
                + "description=" + description
                + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof MyEntity) {
            MyEntity that = (MyEntity) o;
            return (this.id == null ? that.id() == null : this.id.equals(that.id()))
                    && this.metadata.equals(that.metadata())
                    && this.title.equals(that.title())
                    && this.description.equals(that.description());
        }
        return false;
    }

    @Override
    public int hashCode() {
        int h$ = 1;
        h$ *= 1000003;
        h$ ^= (id == null) ? 0 : id.hashCode();
        h$ *= 1000003;
        h$ ^= metadata.hashCode();
        h$ *= 1000003;
        h$ ^= title.hashCode();
        h$ *= 1000003;
        h$ ^= description.hashCode();
        return h$;
    }

    @Override
    public MyEntity.Builder toBuilder() {
        return new Builder(this);
    }

    static final class Builder extends MyEntity.Builder {
        private String id;
        private EntityMetadata metadata;
        private String title;
        private Optional<String> description = Optional.empty();

        Builder() {
        }

        private Builder(MyEntity source) {
            this.id = source.id();
            this.metadata = source.metadata();
            this.title = source.title();
            this.description = source.description();
        }

        @Override
        public MyEntity.Builder id(@Nullable String id) {
            this.id = id;
            return this;
        }

        @Override
        public MyEntity.Builder metadata(EntityMetadata metadata) {
            if (metadata == null) {
                throw new NullPointerException("Null metadata");
            }
            this.metadata = metadata;
            return this;
        }

        @Override
        public MyEntity.Builder title(String title) {
            if (title == null) {
                throw new NullPointerException("Null title");
            }
            this.title = title;
            return this;
        }

        @Override
        public MyEntity.Builder description(@Nullable String description) {
            this.description = Optional.ofNullable(description);
            return this;
        }

        @Override
        public MyEntity build() {
            String missing = "";
            if (this.metadata == null) {
                missing += " metadata";
            }
            if (this.title == null) {
                missing += " title";
            }
            if (!missing.isEmpty()) {
                throw new IllegalStateException("Missing required properties:" + missing);
            }
            return new AutoValue_MyEntity(
                    this.id,
                    this.metadata,
                    this.title,
                    this.description);
        }
    }

}

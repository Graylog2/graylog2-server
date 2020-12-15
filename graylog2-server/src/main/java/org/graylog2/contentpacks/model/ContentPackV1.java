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
package org.graylog2.contentpacks.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.github.zafarkhaja.semver.Version;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import org.bson.types.ObjectId;
import org.graylog2.contentpacks.model.constraints.Constraint;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.contentpacks.model.parameters.Parameter;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@AutoValue
@JsonAutoDetect
@JsonDeserialize(builder = ContentPackV1.Builder.class)
public abstract class ContentPackV1 implements ContentPack {
    static final String VERSION = "1";
    static final String FIELD_NAME = "name";
    static final String FIELD_SUMMARY = "summary";
    static final String FIELD_DESCRIPTION = "description";
    static final String FIELD_VENDOR = "vendor";
    static final String FIELD_URL = "url";
    static final String FIELD_PARAMETERS = "parameters";
    static final String FIELD_ENTITIES = "entities";
    static final String FIELD_CREATED_AT = "created_at";
    static final String FIELD_SERVER_VERSION = "server_version";
    static final String FIELD_DB_ID = "_id";

    @Nullable
    @JsonView(ContentPackView.DBView.class)
    @JsonProperty(FIELD_DB_ID)
    public abstract ObjectId _id();

    @JsonView(ContentPackView.HttpView.class)
    @JsonProperty(FIELD_NAME)
    public abstract String name();

    @JsonView(ContentPackView.HttpView.class)
    @JsonProperty(FIELD_SUMMARY)
    public abstract String summary();

    @JsonView(ContentPackView.HttpView.class)
    @JsonProperty(FIELD_DESCRIPTION)
    public abstract String description();

    @JsonView(ContentPackView.HttpView.class)
    @JsonProperty(FIELD_VENDOR)
    public abstract String vendor();

    @JsonView(ContentPackView.HttpView.class)
    @JsonProperty(FIELD_URL)
    public abstract URI url();

    @JsonView(ContentPackView.HttpView.class)
    @JsonProperty(FIELD_CREATED_AT)
    public abstract DateTime createdAt();

    @JsonView(ContentPackView.HttpView.class)
    @JsonProperty(FIELD_SERVER_VERSION)
    public abstract Version serverVersion();

    @JsonView(ContentPackView.HttpView.class)
    @JsonProperty(FIELD_PARAMETERS)
    public abstract ImmutableSet<Parameter> parameters();

    @JsonView(ContentPackView.HttpView.class)
    @JsonProperty(FIELD_ENTITIES)
    public abstract ImmutableSet<Entity> entities();

    public static Builder builder() {
        return Builder.create();
    }

    public Set<Constraint> constraints() {
        return entities().asList().stream()
                .filter(e -> e instanceof EntityV1)
                .map(e -> (EntityV1) e)
                .map(EntityV1::constraints)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    @AutoValue.Builder
    public abstract static class Builder implements ContentPack.ContentPackBuilder<Builder> {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_ContentPackV1.Builder()
                    .createdAt(DateTime.now(DateTimeZone.UTC))
                    .serverVersion(org.graylog2.plugin.Version.CURRENT_CLASSPATH.getVersion())
                    .parameters(ImmutableSet.of());
        }

        @JsonProperty(FIELD_DB_ID)
        @JsonView(ContentPackView.DBView.class)
        public abstract Builder _id(ObjectId _id);

        @JsonProperty(FIELD_NAME)
        @JsonView(ContentPackView.HttpView.class)
        public abstract Builder name(String name);

        @JsonProperty(FIELD_SUMMARY)
        @JsonView(ContentPackView.HttpView.class)
        public abstract Builder summary(String summary);

        @JsonProperty(FIELD_DESCRIPTION)
        @JsonView(ContentPackView.HttpView.class)
        public abstract Builder description(String description);

        @JsonProperty(FIELD_VENDOR)
        @JsonView(ContentPackView.HttpView.class)
        public abstract Builder vendor(String vendor);

        @JsonProperty(FIELD_URL)
        @JsonView(ContentPackView.HttpView.class)
        public abstract Builder url(URI url);

        @JsonProperty(FIELD_CREATED_AT)
        @JsonView(ContentPackView.HttpView.class)
        public abstract Builder createdAt(DateTime createdAt);

        @JsonProperty(FIELD_SERVER_VERSION)
        @JsonView(ContentPackView.HttpView.class)
        public abstract Builder serverVersion(Version serverVersion);

        @JsonProperty(FIELD_PARAMETERS)
        @JsonView(ContentPackView.HttpView.class)
        public abstract Builder parameters(ImmutableSet<Parameter> parameters);

        @JsonProperty(FIELD_ENTITIES)
        @JsonView(ContentPackView.HttpView.class)
        public abstract Builder entities(ImmutableSet<Entity> entities);

        abstract ContentPackV1 autoBuild();

        public ContentPackV1 build() {
            version(ModelVersion.of(VERSION));
            return autoBuild();
        }
    }
}

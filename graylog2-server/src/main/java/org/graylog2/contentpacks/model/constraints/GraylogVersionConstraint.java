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
package org.graylog2.contentpacks.model.constraints;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.vdurmont.semver4j.Requirement;
import org.graylog2.plugin.Version;

@AutoValue
@JsonDeserialize(builder = AutoValue_GraylogVersionConstraint.Builder.class)
public abstract class GraylogVersionConstraint implements Constraint {
    // TODO: Rename to graylog-version
    public static final String TYPE_NAME = "server-version";
    private static final String FIELD_GRAYLOG_VERSION = "version";

    @JsonProperty(FIELD_GRAYLOG_VERSION)
    public abstract Requirement version();

    public abstract Builder toBuilder();

    public static Builder builder() {
        return new AutoValue_GraylogVersionConstraint.Builder();
    }

    public static GraylogVersionConstraint of(Version version) {
        final String versionString = version.toString().replace("-SNAPSHOT", "");
        final Requirement requirement = Requirement.buildNPM(">=" + versionString);
        return builder()
                .version(requirement)
                .build();
    }

    public static GraylogVersionConstraint currentGraylogVersion() {
        return of(Version.CURRENT_CLASSPATH);
    }

    @AutoValue.Builder
    public abstract static class Builder implements Constraint.ConstraintBuilder<Builder> {
        @JsonProperty(FIELD_GRAYLOG_VERSION)
        public abstract Builder version(Requirement version);

        @JsonIgnore
        public Builder version(String versionExpression) {
            final Requirement requirement = Requirement.buildNPM(versionExpression);
            return version(requirement);
        }

        abstract GraylogVersionConstraint autoBuild();

        public GraylogVersionConstraint build() {
            type(TYPE_NAME);
            return autoBuild();
        }
    }
}

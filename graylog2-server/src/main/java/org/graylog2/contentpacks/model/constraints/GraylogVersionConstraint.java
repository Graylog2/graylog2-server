/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.contentpacks.model.constraints;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonDeserialize(builder = AutoValue_GraylogVersionConstraint.Builder.class)
public abstract class GraylogVersionConstraint implements Constraint {
    // TODO: Rename to graylog-version
    static final String TYPE = "server-version";
    static final String FIELD_GRAYLOG_VERSION = "version";

    // TODO: Build class for version constraint or use com.github.zafarkhaja.semver.expr.Expression?
    @JsonProperty(FIELD_GRAYLOG_VERSION)
    public abstract String version();

    public static Builder builder() {
        return new AutoValue_GraylogVersionConstraint.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder implements Constraint.ConstraintBuilder<Builder> {
        @JsonProperty(FIELD_GRAYLOG_VERSION)
        public abstract Builder version(String version);

        abstract GraylogVersionConstraint autoBuild();

        public GraylogVersionConstraint build() {
            type(TYPE);
            return autoBuild();
        }
    }
}

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
package org.graylog2.cluster;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.hibernate.validator.constraints.NotEmpty;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;

@AutoValue
@WithBeanGetter
public abstract class ClusterConfig {
    @Id
    @ObjectId
    @Nullable
    public abstract String id();

    @JsonProperty
    @Nullable
    public abstract String type();

    @JsonProperty
    @Nullable
    public abstract Object payload();

    @JsonProperty
    @Nullable
    public abstract DateTime lastUpdated();

    @JsonProperty
    @Nullable
    public abstract String lastUpdatedBy();

    @JsonCreator
    public static ClusterConfig create(@Id @ObjectId @JsonProperty("_id") @Nullable String id,
                                       @JsonProperty("type") @Nullable String type,
                                       @JsonProperty("payload") @Nullable Object payload,
                                       @JsonProperty("last_updated") @Nullable DateTime lastUpdated,
                                       @JsonProperty("last_updated_by") @Nullable String lastUpdatedBy) {
        return new AutoValue_ClusterConfig(id, type, payload, lastUpdated, lastUpdatedBy);
    }

    public static ClusterConfig create(@NotEmpty String type,
                                       @NotEmpty Object payload,
                                       @NotEmpty String nodeId) {
        return create(null, type, payload, DateTime.now(DateTimeZone.UTC), nodeId);
    }
}

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
package org.graylog.plugins.sidecar.rest.models;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog2.database.CollectionName;
import org.joda.time.DateTime;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;
import java.util.List;

@AutoValue
@JsonAutoDetect
@CollectionName("collector_actions")
public abstract class CollectorActions {
    @JsonProperty("id")
    @Nullable
    @Id
    @ObjectId
    public abstract String id();

    @JsonProperty("sidecar_id")
    public abstract String sidecarId();

    @JsonProperty("created")
    public abstract DateTime created();

    @JsonProperty("action")
    public abstract List<CollectorAction> action();

    @JsonCreator
    public static CollectorActions create(@JsonProperty("id") @Id @ObjectId String id,
                                          @JsonProperty("sidecar_id") String sidecarId,
                                          @JsonProperty("created") DateTime created,
                                          @JsonProperty("action") List<CollectorAction> action) {
        return new AutoValue_CollectorActions(id, sidecarId, created, action);
    }

    public static CollectorActions create(String sidecar_id,
                                          DateTime created,
                                          List<CollectorAction> action) {
        return create(
                new org.bson.types.ObjectId().toHexString(),
                sidecar_id,
                created,
                action);
    }
}

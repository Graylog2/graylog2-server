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
package org.graylog2.events;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
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
import java.util.Collections;
import java.util.Set;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class ClusterEvent {
    @Id
    @ObjectId
    @Nullable
    public abstract String id();

    @JsonProperty
    public abstract long timestamp();

    @JsonProperty
    @Nullable
    public abstract String producer();

    @JsonProperty
    @Nullable
    public abstract Set<String> consumers();

    @JsonProperty
    @Nullable
    public abstract String eventClass();

    @JsonProperty
    @Nullable
    public abstract Object payload();


    @JsonCreator
    public static ClusterEvent create(@Id @ObjectId @JsonProperty("_id") @Nullable String id,
                                      @JsonProperty("timestamp") long timestamp,
                                      @JsonProperty("producer") @Nullable String producer,
                                      @JsonProperty("consumers") @Nullable Set<String> consumers,
                                      @JsonProperty("event_class") @Nullable String eventClass,
                                      @JsonProperty("payload") @Nullable Object payload) {
        return new AutoValue_ClusterEvent(id, timestamp, producer, consumers, eventClass, payload);
    }

    public static ClusterEvent create(@NotEmpty String producer,
                                      @NotEmpty String eventClass,
                                      @NotEmpty Object payload) {
        return create(null,
                DateTime.now(DateTimeZone.UTC).getMillis(),
                producer,
                Collections.<String>emptySet(),
                eventClass,
                payload);
    }
}

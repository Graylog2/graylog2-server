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
package org.graylog2.messageprocessors;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@AutoValue
@JsonAutoDetect
public abstract class MessageProcessorOrder {

    @JsonProperty
    public abstract long changeVersion();

    @JsonProperty
    public abstract List<String> classOrder();

    @JsonProperty
    public abstract Set<String> disabledMessageProcessors();

    @JsonCreator
    public static MessageProcessorOrder create(@JsonProperty("change_version") long changeVersion,
                                               @JsonProperty("class_order") List<String> classOrder,
                                               @JsonProperty("disabled_message_processors") Set<String> disabledMessageProcessors) {
        return new AutoValue_MessageProcessorOrder(changeVersion, classOrder, disabledMessageProcessors);
    }

    public static MessageProcessorOrder create(long changeVersion, List<String> classOrder) {
        return create(changeVersion, classOrder, Collections.emptySet());
    }
}

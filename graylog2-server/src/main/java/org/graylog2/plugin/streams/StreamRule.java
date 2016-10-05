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
package org.graylog2.plugin.streams;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.graylog2.streams.StreamRuleImpl;

@JsonAutoDetect
public interface StreamRule {
    String getId();

    StreamRuleType getType();

    String getField();

    String getValue();

    Boolean getInverted();

    String getStreamId();

    String getContentPack();

    String getDescription();

    Builder toBuilder();

    interface Builder {
        Builder type(StreamRuleType type);
        Builder field(String field);
        Builder value(String value);
        Builder inverted(Boolean inverted);
        Builder streamId(String streamId);
        Builder contentPack(String contentPack);
        Builder description(String description);
        StreamRule build();
    }
}

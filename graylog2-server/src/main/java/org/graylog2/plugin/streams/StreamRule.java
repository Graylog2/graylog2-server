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
import org.graylog2.plugin.database.Persisted;

import java.util.Map;

@JsonAutoDetect
public interface StreamRule extends Persisted {
    @Override
    String getId();

    StreamRuleType getType();

    String getField();

    String getValue();

    Boolean getInverted();

    String getStreamId();

    String getContentPack();

    String getDescription();

    void setType(StreamRuleType type);

    void setField(String field);

    void setValue(String value);

    void setInverted(Boolean inverted);

    void setContentPack(String contentPack);

    void setDescription(String description);

    @Override
    Map<String, Object> asMap();
}

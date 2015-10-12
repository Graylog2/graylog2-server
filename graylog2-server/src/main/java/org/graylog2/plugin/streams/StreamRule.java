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
    public String getId();

    public StreamRuleType getType();

    public String getField();

    public String getValue();

    public Boolean getInverted();

    public String getStreamId();

    public String getContentPack();

    public void setType(StreamRuleType type);

    public void setField(String field);

    public void setValue(String value);

    public void setInverted(Boolean inverted);

    public void setContentPack(String contentPack);

    public Map<String, Object> asMap();
}

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
package org.graylog2.plugin.lookup;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This is the dummy config that accepts anything and has a marker method to detect a missing plugin.
 * Otherwise loading the config from the database fails hard.
 */
public class FallbackCacheConfig implements LookupCacheConfiguration {

    @JsonProperty
    private String type;

    @Override
    public String type() {
        return type;
    }

    @JsonAnySetter
    public void setType(String key, Object value) {
        // we ignore all the other values, we only want to be able to deserialize unknown configs
    }
}
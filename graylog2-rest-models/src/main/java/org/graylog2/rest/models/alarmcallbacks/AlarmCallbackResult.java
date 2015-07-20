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
package org.graylog2.rest.models.alarmcallbacks;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Map;

@JsonAutoDetect
public abstract class AlarmCallbackResult {
    public abstract String type();

    @JsonCreator
    public static AlarmCallbackResult create(Map<String, Object> result) {
        if (result.get("type") != null) {
            switch(result.get("type").toString().toLowerCase()) {
                case "success": return AlarmCallbackSuccess.create();
                case "error": return AlarmCallbackError.create((String)result.get("error"));
                default: throw new IllegalArgumentException("Invalid AlarmCallbackResult passed. Type was: " + result.get("type"));
            }
        }
        throw new IllegalArgumentException("Invalid AlarmCallbackResult passed. Type was null.");
    }
}

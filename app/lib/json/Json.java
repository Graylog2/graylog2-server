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

package lib.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import lib.Global;

import java.io.IOException;

public class Json extends play.libs.Json {

    protected final static ObjectMapper objectMapper = Global.buildObjectMapper();

    /**
     * Convert an Object to its string representation using the global ObjectMapper.
     * Use this method instead of Json.toJson() to ensure that your Double objects are
     * correctly serialized.
     */
    public static String toJsonString(final Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

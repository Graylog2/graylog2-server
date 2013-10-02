/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.graylog2.inputs.misc.jsonpath;

import com.jayway.jsonpath.JsonPath;

import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class Selector {

    private final JsonPath jsonPath;

    public Selector(JsonPath jsonPath) {
        this.jsonPath = jsonPath;
    }

    public Map<String, Object> read(String json) {
        return jsonPath.read(json);
    }

    public String buildShortMessage(Map<String, Object> fields) {
        StringBuilder shortMessage = new StringBuilder();
        shortMessage.append(jsonPath.getPath()).append(":");
        if (fields.toString().length() > 50) {
            shortMessage.append(fields.toString().substring(50));
        } else {
            shortMessage.append(fields.toString());
        }

        return shortMessage.toString();
    }

}

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
package org.graylog.plugins.views.search.export;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toCollection;

public class TestData {

    public static SimpleMessageChunk simpleMessageChunk(String fieldNames, Object[]... messageValues) {
        LinkedHashSet<SimpleMessage> messages = Arrays.stream(messageValues)
                .map(s -> simpleMessage(fieldNames, s))
                .collect(toCollection(LinkedHashSet::new));
        return SimpleMessageChunk.from(setFrom(fieldNames), messages);
    }

    private static LinkedHashSet<String> setFrom(String fieldNames) {
        return Arrays.stream(fieldNames.split(","))
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public static SimpleMessage simpleMessage(String fieldNames, Object[] values) {
        String[] names = fieldNames.split(",");
        LinkedHashMap<String, Object> fields = new LinkedHashMap<>();
        int i = 0;
        for (String name : names) {
            fields.put(name.trim(), values[i++]);
        }
        return SimpleMessage.from(fields);
    }
}

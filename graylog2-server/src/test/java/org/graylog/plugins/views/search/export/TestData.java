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

import com.google.common.collect.ImmutableSet;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.SearchType;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toCollection;
import static org.graylog.plugins.views.search.TestData.validQueryBuilder;

public class TestData {

    public static Query.Builder validQueryBuilderWith(SearchType searchType) {
        return validQueryBuilder().searchTypes(ImmutableSet.of(searchType));
    }


    public static SimpleMessageChunk simpleMessageChunk(String fieldNames, Object[]... messageValues) {
        LinkedHashSet<SimpleMessage> messages = Arrays.stream(messageValues)
                .map(s -> simpleMessage(fieldNames, s))
                .collect(toCollection(LinkedHashSet::new));
        return SimpleMessageChunk.from(setFrom(fieldNames), messages);
    }

    public static SimpleMessageChunk simpleMessageChunkWithIndexNames(String fieldNames, Object[]... messageValues) {
        LinkedHashSet<SimpleMessage> messages = Arrays.stream(messageValues)
                .map(values -> simpleMessageWithIndexName(fieldNames, values))
                .collect(toCollection(LinkedHashSet::new));
        return SimpleMessageChunk.from(setFrom(fieldNames), messages);
    }

    private static SimpleMessage simpleMessageWithIndexName(String fieldNames, Object[] values) {
        String indexName = (String) values[0];
        Object[] fieldValues = Arrays.copyOfRange(values, 1, values.length);
        return simpleMessage(indexName, fieldNames, fieldValues);
    }

    private static LinkedHashSet<String> setFrom(String fieldNames) {
        return Arrays.stream(fieldNames.split(","))
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public static SimpleMessage simpleMessage(String indexName, String fieldNames, Object[] values) {
        LinkedHashSet<String> names = setFrom(fieldNames);
        LinkedHashMap<String, Object> fields = new LinkedHashMap<>();
        int i = 0;
        for (String name : names) {
            fields.put(name, values[i++]);
        }
        return SimpleMessage.from(indexName, fields);
    }

    public static SimpleMessage simpleMessage(String fieldNames, Object[] values) {
        return simpleMessage("some-index", fieldNames, values);
    }
}

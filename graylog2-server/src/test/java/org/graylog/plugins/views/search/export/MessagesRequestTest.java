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
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import javax.validation.ValidationException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.google.common.collect.Sets.newLinkedHashSet;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class MessagesRequestTest {

    private Map<String, Function<MessagesRequest.Builder, MessagesRequest.Builder>> dummySetters = dummySetters();

    private static Map<String, Function<MessagesRequest.Builder, MessagesRequest.Builder>> dummySetters() {
        HashMap<String, Function<MessagesRequest.Builder, MessagesRequest.Builder>> result = new HashMap<>();
        result.put("timeRange", b -> b.timeRange(someTimeRange()));
        result.put("queryString", b -> b.queryString(ElasticsearchQueryString.empty()));
        result.put("streams", b -> b.streams(ImmutableSet.of("some-stream")));
        result.put("fieldsInOrder", b -> b.fieldsInOrder("some-field"));
        result.put("sort", b -> b.sort(newLinkedHashSet()));
        return result;
    }

    @CsvSource(value = {"timeRange", "queryString", "streams", "fieldsInOrder", "sort"})
    @ParameterizedTest
    void canComplainAboutSingleMissingField(String missingField) {
        MessagesRequest noStreams = requestWithMissingFields(missingField);

        assertThatExceptionOfType(ValidationException.class)
                .isThrownBy(noStreams::ensureCompleteness)
                .withMessageContaining("[" + missingField + "]");
    }

    @Test
    void canComplainAboutMultipleMissingFields() {
        MessagesRequest noStreams = requestWithMissingFields("streams", "sort");

        assertThatExceptionOfType(ValidationException.class)
                .isThrownBy(noStreams::ensureCompleteness)
                .withMessageContaining("[streams, sort]");
    }

    private MessagesRequest requestWithMissingFields(String... missingFieldsArray) {
        List<String> missingFields = Arrays.asList(missingFieldsArray);

        MessagesRequest.Builder builder = MessagesRequest.builder();

        for (String field : dummySetters.keySet())
            if (!missingFields.contains(field))
                builder = dummySetters.get(field).apply(builder);

        return builder.build();
    }

    private static TimeRange someTimeRange() {
        try {
            return RelativeRange.create(1);
        } catch (InvalidRangeParametersException e) {
            throw new RuntimeException(e);
        }
    }
}

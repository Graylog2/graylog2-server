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
package org.graylog2.decorators;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import org.assertj.core.api.Assertions;
import org.graylog2.rest.models.messages.responses.ResultMessageSummary;
import org.graylog2.rest.models.system.indexer.responses.IndexRangeSummary;
import org.graylog2.rest.resources.search.responses.SearchResponse;
import org.joda.time.DateTime;
import org.junit.Test;

import java.util.List;
import java.util.Optional;

public class SyslogSeverityMapperDecoratorTest {
    @Test
    public void testDecorator() throws Exception {
        final DecoratorImpl decorator = DecoratorImpl.create("id",
                SyslogSeverityMapperDecorator.class.getCanonicalName(),
                ImmutableMap.of("source_field", "level", "target_field", "severity"),
                Optional.empty(),
                1);

        final SyslogSeverityMapperDecorator mapperDecorator = new SyslogSeverityMapperDecorator(decorator);

        final IndexRangeSummary indexRangeSummary = IndexRangeSummary.create("graylog_0",
                DateTime.now().minusDays(1),
                DateTime.now(),
                null,
                100);

        final List<ResultMessageSummary> messages = ImmutableList.of(
                ResultMessageSummary.create(ImmutableMultimap.of(), ImmutableMap.of("_id", "h", "level", "80"), "graylog_0"),
                ResultMessageSummary.create(ImmutableMultimap.of(), ImmutableMap.of("_id", "a", "level", "0"), "graylog_0"),
                ResultMessageSummary.create(ImmutableMultimap.of(), ImmutableMap.of("_id", "b", "level", "1"), "graylog_0"),
                ResultMessageSummary.create(ImmutableMultimap.of(), ImmutableMap.of("_id", "c", "level", "2"), "graylog_0"),
                ResultMessageSummary.create(ImmutableMultimap.of(), ImmutableMap.of("_id", "d", "level", "3"), "graylog_0"),
                ResultMessageSummary.create(ImmutableMultimap.of(), ImmutableMap.of("_id", "e", "level", "4"), "graylog_0"),
                ResultMessageSummary.create(ImmutableMultimap.of(), ImmutableMap.of("_id", "f", "level", "5"), "graylog_0"),
                ResultMessageSummary.create(ImmutableMultimap.of(), ImmutableMap.of("_id", "g", "level", "6"), "graylog_0"),
                ResultMessageSummary.create(ImmutableMultimap.of(), ImmutableMap.of("_id", "h", "level", "7"), "graylog_0"),
                ResultMessageSummary.create(ImmutableMultimap.of(), ImmutableMap.of("_id", "i", "foo", "1"), "graylog_0")
        );

        final SearchResponse searchResponse = SearchResponse.builder()
                .query("foo")
                .builtQuery("foo")
                .usedIndices(ImmutableSet.of(indexRangeSummary))
                .messages(messages)
                .fields(ImmutableSet.of("level"))
                .time(100L)
                .totalResults(messages.size())
                .from(DateTime.now().minusHours(1))
                .to(DateTime.now())
                .build();

        final SearchResponse response = mapperDecorator.apply(searchResponse);

        // Returns the value if the value cannot be mapped to a Syslog severity
        Assertions.assertThat(response.messages().get(0).message().get("level")).isEqualTo("80");
        Assertions.assertThat(response.messages().get(0).message().get("severity")).isNull();

        // Check that the mapping works correctly
        Assertions.assertThat(response.messages().get(1).message().get("level")).isEqualTo("0");
        Assertions.assertThat(response.messages().get(1).message().get("severity")).isEqualTo("Emergency (0)");
        Assertions.assertThat(response.messages().get(2).message().get("level")).isEqualTo("1");
        Assertions.assertThat(response.messages().get(2).message().get("severity")).isEqualTo("Alert (1)");
        Assertions.assertThat(response.messages().get(3).message().get("level")).isEqualTo("2");
        Assertions.assertThat(response.messages().get(3).message().get("severity")).isEqualTo("Critical (2)");
        Assertions.assertThat(response.messages().get(4).message().get("level")).isEqualTo("3");
        Assertions.assertThat(response.messages().get(4).message().get("severity")).isEqualTo("Error (3)");
        Assertions.assertThat(response.messages().get(5).message().get("level")).isEqualTo("4");
        Assertions.assertThat(response.messages().get(5).message().get("severity")).isEqualTo("Warning (4)");
        Assertions.assertThat(response.messages().get(6).message().get("level")).isEqualTo("5");
        Assertions.assertThat(response.messages().get(6).message().get("severity")).isEqualTo("Notice (5)");
        Assertions.assertThat(response.messages().get(7).message().get("level")).isEqualTo("6");
        Assertions.assertThat(response.messages().get(7).message().get("severity")).isEqualTo("Informational (6)");
        Assertions.assertThat(response.messages().get(8).message().get("level")).isEqualTo("7");
        Assertions.assertThat(response.messages().get(8).message().get("severity")).isEqualTo("Debug (7)");

        // If the message does not have a source field, we do not touch it
        Assertions.assertThat(response.messages().get(9).message().get("level")).isNull();
        Assertions.assertThat(response.messages().get(9).message().get("severity")).isNull();
        Assertions.assertThat(response.messages().get(9).message().get("foo")).isEqualTo("1");
    }

    @Test(expected = NullPointerException.class)
    public void testNullSourceField() throws Exception {
        final DecoratorImpl decorator = DecoratorImpl.create("id",
                SyslogSeverityMapperDecorator.class.getCanonicalName(),
                ImmutableMap.of("target_field", "severity"),
                Optional.empty(),
                1);

        new SyslogSeverityMapperDecorator(decorator);
    }

    @Test(expected = NullPointerException.class)
    public void testNullTargetField() throws Exception {
        final DecoratorImpl decorator = DecoratorImpl.create("id",
                SyslogSeverityMapperDecorator.class.getCanonicalName(),
                ImmutableMap.of("source_field", "level"),
                Optional.empty(),
                1);

        new SyslogSeverityMapperDecorator(decorator);
    }
}
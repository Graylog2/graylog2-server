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
package org.graylog2.alerts.types;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.graylog2.Configuration;
import org.graylog2.alerts.AbstractAlertCondition;
import org.graylog2.alerts.AlertConditionTest;
import org.graylog2.indexer.ranges.IndexRange;
import org.graylog2.indexer.results.SearchResult;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.indexer.searches.Sorting;
import org.graylog2.indexer.searches.timeranges.RelativeRange;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.alarms.AlertCondition;
import org.graylog2.plugin.streams.Stream;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class FieldContentValueAlertConditionTest extends AlertConditionTest {

    @Test
    public void testConstructor() throws Exception {
        final Map<String, Object> parameters = getParametersMap(0, "field", "value");

        final FieldContentValueAlertCondition condition = getCondition(parameters);

        assertNotNull(condition);
        assertNotNull(condition.getDescription());
    }

    @Test
    public void testRunMatchingMessagesInStream() throws Exception {
        final SearchHits searchHits = mock(SearchHits.class);

        final SearchHit searchHit = mock(SearchHit.class);
        final HashMap<String, Object> source = Maps.newHashMap();
        source.put("message", "something is in here");

        when(searchHit.getSource()).thenReturn(source);
        when(searchHit.getIndex()).thenReturn("graylog_test");
        when(searchHits.iterator()).thenReturn(Iterators.singletonIterator(searchHit));

        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final IndexRange indexRange = IndexRange.create("graylog_test", now.minusDays(1), now, now, 0);
        final Set<IndexRange> indexRanges = Sets.newHashSet(indexRange);
        final SearchResult searchResult = spy(new SearchResult(searchHits,
                                                           indexRanges,
                                                           "message:something",
                                                           null,
                                                           new TimeValue(100, TimeUnit.MILLISECONDS)));
        when(searchResult.getTotalResults()).thenReturn(1L);
        when(searches.search(
                anyString(),
                anyString(),
                any(RelativeRange.class),
                anyInt(),
                anyInt(),
                any(Sorting.class)))
            .thenReturn(searchResult);
        final FieldContentValueAlertCondition condition = getCondition(getParametersMap(0, "message", "something"));

        alertLastTriggered(-1);

        final AlertCondition.CheckResult result = alertService.triggered(condition);

        assertTriggered(condition, result);
    }


    @Test
    public void testRunNoMatchingMessages() throws Exception {
        final SearchHits searchHits = mock(SearchHits.class);
        when(searchHits.iterator()).thenReturn(Collections.<SearchHit>emptyIterator());

        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final IndexRange indexRange = IndexRange.create("graylog_test", now.minusDays(1), now, now, 0);
        final Set<IndexRange> indexRanges = Sets.newHashSet(indexRange);
        final SearchResult searchResult = spy(new SearchResult(searchHits,
                                                               indexRanges,
                                                               "message:something",
                                                               null,
                                                               new TimeValue(100, TimeUnit.MILLISECONDS)));
        when(searches.search(
                anyString(),
                anyString(),
                any(RelativeRange.class),
                anyInt(),
                anyInt(),
                any(Sorting.class)))
                .thenReturn(searchResult);
        final FieldContentValueAlertCondition condition = getCondition(getParametersMap(0, "message", "something"));

        alertLastTriggered(-1);

        final AlertCondition.CheckResult result = alertService.triggered(condition);

        assertNotTriggered(result);
    }

    @Test
    public void testCorrectUsageOfRelativeRange() throws Exception {
        final Stream stream = mock(Stream.class);
        final Searches searches = mock(Searches.class);
        final Configuration configuration = mock(Configuration.class);
        final SearchResult searchResult = mock(SearchResult.class);
        final int alertCheckInterval = 42;
        final RelativeRange relativeRange = new RelativeRange(alertCheckInterval);

        when(configuration.getAlertCheckInterval()).thenReturn(alertCheckInterval);

        when(searches.search(anyString(),
                anyString(),
                eq(relativeRange),
                anyInt(),
                anyInt(),
                any(Sorting.class))).thenReturn(searchResult);

        final FieldContentValueAlertCondition alertCondition = new FieldContentValueAlertCondition(searches, configuration, stream,
                null, DateTime.now(DateTimeZone.UTC), "mockuser", ImmutableMap.<String,Object>of("field", "test", "value", "test"));

        final AbstractAlertCondition.CheckResult result = alertCondition.runCheck();
    }

    protected FieldContentValueAlertCondition getCondition(Map<String, Object> parameters) {
        return new FieldContentValueAlertCondition(
                searches,
                mock(Configuration.class),
                stream,
                CONDITION_ID,
                Tools.iso8601(),
                STREAM_CREATOR,
                parameters);
    }

    protected Map<String, Object> getParametersMap(Integer grace, String field, String value) {
        Map<String, Object> parameters = new HashMap<>();

        parameters.put("grace", grace);
        parameters.put("field", field);
        parameters.put("value", value);

        return parameters;
    }

}

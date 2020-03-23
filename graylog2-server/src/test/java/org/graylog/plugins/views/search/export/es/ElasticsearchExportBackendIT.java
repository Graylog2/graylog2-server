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
package org.graylog.plugins.views.search.export.es;

import com.google.common.collect.ImmutableSet;
import org.graylog.plugins.views.search.elasticsearch.IndexLookup;
import org.graylog.plugins.views.search.export.Defaults;
import org.graylog.plugins.views.search.export.MessagesRequest;
import org.graylog.testing.elasticsearch.ElasticsearchBaseTest;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class ElasticsearchExportBackendIT extends ElasticsearchBaseTest {

    private IndexLookup indexLookup;
    private ElasticsearchExportBackend sut;

    @Before
    public void setUp() {
        indexLookup = mock(IndexLookup.class);
        sut = new ElasticsearchExportBackend(jestClient(), indexLookup);
    }

    @Test
    public void performsSearchAfterRequest() {
        importFixture("messages.json");

        MessagesRequest defaultRequest = new Defaults().fillInIfNecessary(MessagesRequest.empty());

        ImmutableSet<String> streams = ImmutableSet.of("000000000000000000000001");

        TimeRange timeRange = allMessagesTimeRange();

        MessagesRequest request = defaultRequest.toBuilder()
                .timeRange(timeRange)
                .streams(streams)
                .build();

        when(indexLookup.indexNamesForStreamsInTimeRange(streams, timeRange))
                .thenReturn(ImmutableSet.of("graylog_0"));

        sut.run(request);
    }

    private TimeRange allMessagesTimeRange() {
        try {
            return AbsoluteRange.create("2015-01-01 00:00:00.000", "2015-01-03 00:00:00.000");
        } catch (InvalidRangeParametersException e) {
            throw new RuntimeException(e);
        }
    }
}

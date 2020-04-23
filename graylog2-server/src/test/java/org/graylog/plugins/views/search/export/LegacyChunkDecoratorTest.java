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

import org.assertj.core.groups.Tuple;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.elasticsearch.searchtypes.pivot.LegacyDecoratorProcessor;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.rest.models.messages.responses.ResultMessageSummary;
import org.graylog2.rest.resources.search.responses.SearchResponse;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog.plugins.views.search.export.LinkedHashSetUtil.linkedHashSetOf;
import static org.graylog.plugins.views.search.export.TestData.simpleMessageChunkWithIndexNames;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LegacyChunkDecoratorTest {

    private LegacyChunkDecorator sut;
    private LegacyDecoratorProcessor decoratorProcessor;

    @BeforeEach
    void setUp() {
        decoratorProcessor = mock(LegacyDecoratorProcessor.class);
        sut = new LegacyChunkDecorator(decoratorProcessor);
    }

    @Test
    void setsMessagesOnSearchResponse() {
        SimpleMessageChunk undecoratedChunk = simpleMessageChunkWithIndexNames("field-1,field-2",
                new Object[]{"index-1", "1", "a"},
                new Object[]{"index-2", "2", "b"});

        SearchResponse builtLegacyResponse = captureLegacyResponse(undecoratedChunk, validCommand());

        assertThat(builtLegacyResponse.messages())
                .extracting(ResultMessageSummary::index, m -> m.message().get("field-1"), m -> m.message().get("field-2"))
                .containsExactly(
                        Tuple.tuple("index-1", "1", "a"),
                        Tuple.tuple("index-2", "2", "b"));
    }

    // these fields are only set in case a user has custom decorators that rely on any of the other fields in SearchResponse
    // our own decorators don't use them.
    @Test
    void setsPlausibleAdditionalValuesOnLegacySearchResponse() {
        SimpleMessageChunk undecoratedChunk = SimpleMessageChunk.from(linkedHashSetOf("field-1", "field-2"));

        ExportMessagesCommand command = validCommand(ElasticsearchQueryString.builder().queryString("hase").build());

        SearchResponse builtLegacyResponse = captureLegacyResponse(undecoratedChunk, command);

        assertThat(builtLegacyResponse.from()).isEqualTo(command.timeRange().getFrom());
        assertThat(builtLegacyResponse.to()).isEqualTo(command.timeRange().getTo());
        assertThat(builtLegacyResponse.query()).isEqualTo("hase");
        assertThat(builtLegacyResponse.builtQuery()).isEqualTo("hase");
        assertThat(builtLegacyResponse.fields()).containsExactlyElementsOf(undecoratedChunk.fieldsInOrder());
    }

    @Test
    void setsClearDefaultsForValuesThatCantBeProperlyDetermined() {
        SimpleMessageChunk undecoratedChunk = SimpleMessageChunk.from(linkedHashSetOf());

        ExportMessagesCommand command = validCommand();

        SearchResponse builtLegacyResponse = captureLegacyResponse(undecoratedChunk, command);

        assertThat(builtLegacyResponse.totalResults())
                .as("total results can't be determined from a single chunk").isEqualTo(-1);
        assertThat(builtLegacyResponse.time())
                .as("total execution time be determined from a single chunk").isEqualTo(-1);
        assertThat(builtLegacyResponse.usedIndices())
                .as("index data is omitted to save the overhead of loading it").isEmpty();
    }

    private ExportMessagesCommand validCommand() {
        return validCommand(ElasticsearchQueryString.empty());
    }

    private ExportMessagesCommand validCommand(ElasticsearchQueryString queryString) {
        return ExportMessagesCommand.builder()
                .timeRange(someTimeRange())
                .queryString(queryString)
                .build();
    }

    private SearchResponse captureLegacyResponse(SimpleMessageChunk undecoratedChunk, ExportMessagesCommand command) {
        ArgumentCaptor<SearchResponse> captor = ArgumentCaptor.forClass(SearchResponse.class);
        when(decoratorProcessor.decorateSearchResponse(captor.capture(), any())).thenReturn(mock(SearchResponse.class));


        sut.decorate(undecoratedChunk, new ArrayList<>(), command);

        return captor.getValue();
    }

    private AbsoluteRange someTimeRange() {
        return AbsoluteRange.create(DateTime.now(DateTimeZone.UTC), DateTime.now(DateTimeZone.UTC).plus(300));
    }
}

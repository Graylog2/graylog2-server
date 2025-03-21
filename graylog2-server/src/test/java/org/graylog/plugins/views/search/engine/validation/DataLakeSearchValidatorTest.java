package org.graylog.plugins.views.search.engine.validation;

import com.google.common.collect.ImmutableSet;
import org.graylog.plugins.views.search.Filter;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.engine.BackendQuery;
import org.graylog.plugins.views.search.errors.SearchError;
import org.graylog.plugins.views.search.errors.SearchTypeError;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.search.searchfilters.model.UsedSearchFilter;
import org.graylog.plugins.views.search.searchtypes.DataLakeSearchType;
import org.graylog.plugins.views.search.searchtypes.MessageList;
import org.graylog.plugins.views.search.searchtypes.events.EventList;
import org.graylog2.plugin.streams.Stream;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class DataLakeSearchValidatorTest {
    @Test
    void testReturnsEmptySetOfErrorsWhenValidatingIndexerSearch() {
        final Query indexerOnlyQuery = testQuery(ElasticsearchQueryString.of("GET"),
                Set.of(
                        MessageList.builder().build(),
                        EventList.builder().build()
                )
        );
        final Search indexerSearch = Search.builder()
                .id("nvmd")
                .queries(ImmutableSet.of(indexerOnlyQuery))
                .build();
        assertFalse(DataLakeSearchValidator.containsDataLakeSearchElements(indexerSearch));
        assertFalse(DataLakeSearchValidator.containsDataLakeSearchElements(indexerOnlyQuery));
        assertTrue(new DataLakeSearchValidator()
                .validate(indexerSearch, mock(SearchUser.class))
                .isEmpty());
    }

    @Test
    void testReturnsErrorWhenValidatingIndexerSearchWithDataLakeSearchType() {
        final Query indexerQueryWithUnexpectedDataLakeSearchType = testQuery(ElasticsearchQueryString.of("GET"),
                Set.of(
                        MessageList.builder().build(),
                        EventList.builder().build(),
                        mockDataLakeSearchType()
                )
        );
        final Search indexerSearchWithDataLakeSearchType = Search.builder()
                .id("nvmd")
                .queries(ImmutableSet.of(indexerQueryWithUnexpectedDataLakeSearchType))
                .build();
        assertTrue(DataLakeSearchValidator.containsDataLakeSearchElements(indexerSearchWithDataLakeSearchType));
        assertTrue(DataLakeSearchValidator.containsDataLakeSearchElements(indexerQueryWithUnexpectedDataLakeSearchType));
        assertFalse(new DataLakeSearchValidator()
                .validate(indexerSearchWithDataLakeSearchType, mock(SearchUser.class))
                .isEmpty());
    }

    @Test
    void testReturnsErrorWhenValidatingDataLakeSearchWithIndexerSearchType() {
        final Query dataLakeQueryWithUnexpectedIndexerSearchType = testQuery(mockDataLakeBackend(),
                Set.of(
                        mockDataLakeSearchType(),
                        MessageList.builder().build()
                )
        );
        final Search dataLakeSearchWithUnexpectedIndexerSearchType = Search.builder()
                .id("nvmd")
                .queries(ImmutableSet.of(dataLakeQueryWithUnexpectedIndexerSearchType))
                .build();
        assertTrue(DataLakeSearchValidator.containsDataLakeSearchElements(dataLakeSearchWithUnexpectedIndexerSearchType));
        assertTrue(DataLakeSearchValidator.containsDataLakeSearchElements(dataLakeQueryWithUnexpectedIndexerSearchType));
        assertFalse(new DataLakeSearchValidator()
                .validate(dataLakeSearchWithUnexpectedIndexerSearchType, mock(SearchUser.class))
                .isEmpty());
    }

    @Test
    void testReturnsErrorWhenValidatingDataLakeSearchWithMoreThanOneSearchType() {
        final Query queryWithTwoDataLakeSearchTypes = testQuery(mockDataLakeBackend(),
                Set.of(
                        mockDataLakeSearchType(),
                        mockDataLakeSearchType()
                )
        );
        final Search dataLakeSearchWithTwoDataLakeSearchTypes = Search.builder()
                .id("nvmd")
                .queries(ImmutableSet.of(queryWithTwoDataLakeSearchTypes))
                .build();
        assertTrue(DataLakeSearchValidator.containsDataLakeSearchElements(dataLakeSearchWithTwoDataLakeSearchTypes));
        assertTrue(DataLakeSearchValidator.containsDataLakeSearchElements(queryWithTwoDataLakeSearchTypes));
        assertFalse(new DataLakeSearchValidator()
                .validate(dataLakeSearchWithTwoDataLakeSearchTypes, mock(SearchUser.class))
                .isEmpty());
    }

    @Test
    void testReturnsErrorWhenValidatingDataLakeSearchWithSearchTypeWithManyStreams() {
        final DataLakeSearchType crappySearchType = mockDataLakeSearchType();
        doReturn(Set.of(mock(Stream.class), mock(Stream.class))).when(crappySearchType).streams();
        final Query queryWithManyStreams = testQuery(
                mockDataLakeBackend(),
                Set.of(crappySearchType)
        );
        final Search dataLakeSearchWithManyStreams = Search.builder()
                .id("nvmd")
                .queries(ImmutableSet.of(queryWithManyStreams))
                .build();
        assertTrue(DataLakeSearchValidator.containsDataLakeSearchElements(dataLakeSearchWithManyStreams));
        assertTrue(DataLakeSearchValidator.containsDataLakeSearchElements(queryWithManyStreams));
        assertFalse(new DataLakeSearchValidator()
                .validate(dataLakeSearchWithManyStreams, mock(SearchUser.class))
                .isEmpty());
    }

    @Test
    void testMultipleErrorsScenario() {
        final DataLakeSearchType crappySearchType = mockDataLakeSearchType();
        doReturn("createdByAi").when(crappySearchType).id();
        doReturn(Set.of(mock(Stream.class), mock(Stream.class))).when(crappySearchType).streams(); //1. Search type error: 2 streams
        doReturn(Set.of("Heavy Stream Category")).when(crappySearchType).streamCategories(); //2. Search type error: stream categories forbidden
        doReturn(List.of(mock(UsedSearchFilter.class))).when(crappySearchType).filters(); //3. Search type error: stream categories forbidden
        doReturn(mock(Filter.class)).when(crappySearchType).filter(); //4. Search type error: stream categories forbidden
        final Query queryWithManyErrors = testQuery(
                ElasticsearchQueryString.of("GET"), //Query error : Wrong backend
                Set.of(crappySearchType, mockDataLakeSearchType()) //Query error: Too many search types
        );
        final Search search = Search.builder()
                .id("nvmd")
                .queries(ImmutableSet.of(queryWithManyErrors))
                .build();
        assertTrue(DataLakeSearchValidator.containsDataLakeSearchElements(search));
        assertTrue(DataLakeSearchValidator.containsDataLakeSearchElements(queryWithManyErrors));
        final Set<SearchError> errors = new DataLakeSearchValidator()
                .validate(search, mock(SearchUser.class));
        assertEquals(6, errors.size());
        assertEquals(4, errors.stream()
                .filter(err -> err instanceof SearchTypeError)
                .filter(searchError -> ((SearchTypeError) searchError).searchTypeId().equals("createdByAi"))
                .count());
    }

    private Query testQuery(final BackendQuery backendQuery,
                            final Set<SearchType> searchTypes) {
        return Query.builder()
                .id("nvmd")
                .query(backendQuery)
                .searchTypes(searchTypes)
                .build();
    }

    private DataLakeSearchType mockDataLakeSearchType() {
        return mock(DataLakeSearchType.class);
    }

    private BackendQuery mockDataLakeBackend() {
        final BackendQuery backendQuery = mock(BackendQuery.class);
        doReturn(DataLakeSearchType.PREFIX + "iceberg").when(backendQuery).type();
        return backendQuery;
    }
}

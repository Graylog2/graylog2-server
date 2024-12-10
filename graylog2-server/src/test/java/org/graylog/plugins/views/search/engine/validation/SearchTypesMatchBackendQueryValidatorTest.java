package org.graylog.plugins.views.search.engine.validation;

import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.errors.SearchError;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.search.searchtypes.MessageList;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.Values;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog.plugins.views.search.engine.validation.SearchTypesMatchBackendQueryValidator.INVALID_SEARCH_TYPE_FOR_GIVEN_QUERY_TYPE_MSG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class SearchTypesMatchBackendQueryValidatorTest {

    private SearchTypesMatchBackendQueryValidator toTest;
    @Mock
    private SearchUser searchUser;

    @BeforeEach
    void setUp() {
        toTest = new SearchTypesMatchBackendQueryValidator();
    }

    @Test
    void testValidationIsOkOnProperSearchTypes() {
        Query query = Query.builder()
                .query(ElasticsearchQueryString.of("bayobongo"))
                .searchTypes(Set.of(
                        MessageList.builder()
                                .id("messageListId")
                                .build(),

                        Pivot.builder()
                                .rowGroups(
                                        Values.builder()
                                                .fields(List.of("id", "whatever"))
                                                .limit(10)
                                                .build()
                                )
                                .series(List.of())
                                .rollup(false)
                                .build()
                ))
                .build();

        assertEquals(Set.of(), toTest.validate(query, searchUser));
    }

    @Test
    void testValidationDiscoversWrongSearchTypes() {
        final SearchType wrongSearchType = mock(SearchType.class);
        doReturn("wrong!").when(wrongSearchType).id();
        Query query = Query.builder()
                .id("query_id")
                .query(ElasticsearchQueryString.of("bayobongo"))
                .searchTypes(Set.of(
                        MessageList.builder()
                                .id("messageListId")
                                .build(),

                        wrongSearchType
                ))
                .build();

        final Set<SearchError> errors = toTest.validate(query, searchUser);
        assertThat(errors)
                .isNotNull()
                .hasSize(1);
        assertThat(errors.stream().findFirst().get().description())
                .isEqualTo(INVALID_SEARCH_TYPE_FOR_GIVEN_QUERY_TYPE_MSG);

    }
}

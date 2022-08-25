package org.graylog.plugins.views.search.searchfilters;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.TestData;
import org.graylog.plugins.views.search.searchfilters.model.InlineQueryStringSearchFilter;
import org.graylog.plugins.views.search.searchfilters.model.ReferencedQueryStringSearchFilter;
import org.graylog.plugins.views.search.searchfilters.model.ReferencedSearchFilter;
import org.graylog.plugins.views.search.views.UnknownWidgetConfigDTO;
import org.graylog.plugins.views.search.views.WidgetDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReferencedSearchFiltersRetrieverTest {

    private ReferencedSearchFiltersRetriever toTest;

    @BeforeEach
    void setUp() {
        toTest = new ReferencedSearchFiltersRetriever();
    }

    @Test
    void testGetReferencedSearchFiltersIdsReturnsEmptyCollectionOnEmptyOwners() {
        final Set<String> referencedSearchFiltersIds = toTest.getReferencedSearchFiltersIds(ImmutableSet.of());
        assertTrue(referencedSearchFiltersIds.isEmpty());
    }

    @Test
    void testGetReferencedSearchFiltersIdsReturnsEmptyCollectionOnOwnersWithoutFilters() {
        final Query query = TestData.validQueryBuilder().build();
        final WidgetDTO widget = WidgetDTO.builder().id("nvmd").type("nvmd").config(UnknownWidgetConfigDTO.create(Collections.emptyMap())).build();
        final Set<String> referencedSearchFiltersIds = toTest.getReferencedSearchFiltersIds(ImmutableSet.of(query, widget));
        assertTrue(referencedSearchFiltersIds.isEmpty());
    }

    @Test
    void testGetReferencedSearchFiltersIdsDoesNotReturnInlinedSearchFilters() {
        final Query query = TestData.validQueryBuilder()
                .filters(ImmutableList.of(
                        InlineQueryStringSearchFilter.builder().queryString("nvmd").build(),
                        InlineQueryStringSearchFilter.builder().queryString("nvmd2").build())
                )
                .build();

        final Set<String> referencedSearchFiltersIds = toTest.getReferencedSearchFiltersIds(ImmutableSet.of(query));
        assertTrue(referencedSearchFiltersIds.isEmpty());
    }

    @Test
    void testGetReferencedSearchFiltersIdsReturnsProperIds() {
        final ReferencedSearchFilter filter1 = ReferencedQueryStringSearchFilter.builder().id("r_id_1").build();
        final ReferencedSearchFilter filter2 = ReferencedQueryStringSearchFilter.builder().id("r_id_2").build();
        final Query query = TestData.validQueryBuilder()
                .filters(ImmutableList.of(filter1, filter2))
                .build();
        final Set<String> referencedSearchFiltersIds = toTest.getReferencedSearchFiltersIds(ImmutableSet.of(query));
        assertEquals(ImmutableSet.of("r_id_1", "r_id_2"), referencedSearchFiltersIds);
    }

}

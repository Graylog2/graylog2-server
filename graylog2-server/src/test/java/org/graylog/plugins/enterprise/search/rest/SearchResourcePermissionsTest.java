package org.graylog.plugins.enterprise.search.rest;

import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.rest.SearchResource;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SearchResourcePermissionsTest {
    private SearchResource resourceToTestIsOwnerOfSearch() {
        final SearchResource resource = mock(SearchResource.class);
        when(resource.isOwnerOfSearch(any(), any())).thenCallRealMethod();
        return resource;
    }

    @Test
    public void exactUserOfSearchIsOwner() {
        final SearchResource resource = resourceToTestIsOwnerOfSearch();
        final String username = "karl";
        final Search search = Search.builder().owner(username).build();

        assertThat(resource.isOwnerOfSearch(search, username)).isTrue();
    }

    @Test
    public void anyUserIsOwnerOfLegacySearchesWithoutOwner() {
        final SearchResource resource = resourceToTestIsOwnerOfSearch();
        final String username = "karl";
        final Search search = Search.builder().build();

        assertThat(resource.isOwnerOfSearch(search, username)).isTrue();
    }

    @Test
    public void usernameNotMatchingIsNotOwner() {
        final SearchResource resource = resourceToTestIsOwnerOfSearch();
        final String username = "karl";
        final Search search = Search.builder().owner("friedrich").build();

        assertThat(resource.isOwnerOfSearch(search, username)).isFalse();
    }
}

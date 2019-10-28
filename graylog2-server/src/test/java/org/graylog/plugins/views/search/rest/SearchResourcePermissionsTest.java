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
package org.graylog.plugins.views.search.rest;

import org.graylog.plugins.views.search.Search;
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

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
package org.graylog.plugins.views.search.views;

import com.google.common.collect.ImmutableSet;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchRequiresParameterSupport;
import org.graylog.plugins.views.search.ValueParameter;
import org.graylog.plugins.views.search.db.SearchDbService;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class RequiresParameterSupportTest {
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Mock
    private SearchDbService searchDbService;

    private RequiresParameterSupport requiresParameterSupport;

    private ViewDTO view;

    @Before
    public void setUp() throws Exception {
        this.requiresParameterSupport = new RequiresParameterSupport(searchDbService, new SearchRequiresParameterSupport(new EnterpriseMetadataSummary()));

        this.view = ViewDTO.builder()
                .title("Sample View")
                .state(Collections.emptyMap())
                .searchId("searchId")
                .build();
    }

    @Test
    public void throwsExceptionIfSearchIsMissing() {
        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage(Matchers.allOf(
                Matchers.startsWith("Search searchId for view"),
                Matchers.endsWith("is missing.")
        ));

        when(searchDbService.get("searchId")).thenReturn(Optional.empty());

        this.requiresParameterSupport.test(view);
    }

    @Test
    public void returnsEmptyCapabilitiesIfViewDoesNotHaveParameters() {
        final Search search = Search.builder().parameters(ImmutableSet.of()).build();

        when(searchDbService.get("searchId")).thenReturn(Optional.of(search));

        final Map<String, PluginMetadataSummary> result = this.requiresParameterSupport.test(view);

        assertThat(result).isEmpty();
    }

    @Test
    public void returnsParameterCapabilityIfViewDoesHaveParameters() {
        final Search search = Search.builder().parameters(ImmutableSet.of(
                ValueParameter.builder()
                        .name("foo")
                        .dataType("any")
                        .build()
        )).build();

        when(searchDbService.get("searchId")).thenReturn(Optional.of(search));

        final Map<String, PluginMetadataSummary> result = this.requiresParameterSupport.test(view);

        assertThat(result).containsExactly(
                new AbstractMap.SimpleEntry("parameters", new EnterpriseMetadataSummary())
        );
    }
}

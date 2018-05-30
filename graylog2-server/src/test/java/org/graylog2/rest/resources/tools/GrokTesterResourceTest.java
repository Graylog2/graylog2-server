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
package org.graylog2.rest.resources.tools;

import org.graylog2.events.ClusterEventBus;
import org.graylog2.grok.GrokPattern;
import org.graylog2.grok.InMemoryGrokPatternService;
import org.graylog2.rest.resources.tools.responses.GrokTesterResponse;
import org.graylog2.shared.bindings.GuiceInjectorHolder;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

public class GrokTesterResourceTest {
    static {
        GuiceInjectorHolder.createInjector(Collections.emptyList());
    }

    private ClusterEventBus clusterEventBus;
    private GrokTesterResource resource;

    @Before
    public void setUp() throws Exception {
        clusterEventBus = new ClusterEventBus("cluster-event-bus", Executors.newSingleThreadExecutor());
        final InMemoryGrokPatternService grokPatternService = new InMemoryGrokPatternService(clusterEventBus);
        grokPatternService.save(GrokPattern.create("NUMBER", "[0-9]+"));
        resource = new GrokTesterResource(grokPatternService);
    }

    @Test
    public void testGrokWithValidPatternAndMatch() {
        final GrokTesterResponse response = resource.grokTest("%{NUMBER}", "abc 1234", false);
        assertThat(response.matched()).isTrue();
        assertThat(response.pattern()).isEqualTo("%{NUMBER}");
        assertThat(response.string()).isEqualTo("abc 1234");
        assertThat(response.matches()).containsOnly(GrokTesterResponse.Match.create("NUMBER", "1234"));
        assertThat(response.errorMessage()).isNullOrEmpty();
    }

    @Test
    public void testGrokWithValidPatternAndNoMatch() {
        final GrokTesterResponse response = resource.grokTest("%{NUMBER}", "abc def", false);
        assertThat(response.matched()).isFalse();
        assertThat(response.pattern()).isEqualTo("%{NUMBER}");
        assertThat(response.string()).isEqualTo("abc def");
        assertThat(response.matches()).isEmpty();
        assertThat(response.errorMessage()).isNullOrEmpty();
    }

    @Test
    public void testGrokWithInvalidPattern() {
        final GrokTesterResponse response = resource.grokTest("%{NUMBER", "abc 1234", false);
        assertThat(response.matched()).isFalse();
        assertThat(response.pattern()).isEqualTo("%{NUMBER");
        assertThat(response.string()).isEqualTo("abc 1234");
        assertThat(response.errorMessage()).startsWith("Illegal repetition near index 0");
    }

    @Test
    public void testGrokWithMissingPattern() {
        final GrokTesterResponse response = resource.grokTest("%{FOOBAR} %{NUMBER}", "abc 1234", false);
        assertThat(response.matched()).isFalse();
        assertThat(response.pattern()).isEqualTo("%{FOOBAR} %{NUMBER}");
        assertThat(response.string()).isEqualTo("abc 1234");
        assertThat(response.errorMessage()).isEqualTo("No definition for key 'FOOBAR' found, aborting");
    }

    @Test
    public void testGrokWithEmptyPattern() {
        final GrokTesterResponse response = resource.grokTest("", "abc 1234", false);
        assertThat(response.matched()).isFalse();
        assertThat(response.pattern()).isEqualTo("");
        assertThat(response.string()).isEqualTo("abc 1234");
        assertThat(response.errorMessage()).isEqualTo("{pattern} should not be empty or null");
    }

    @Test
    public void testGrokWithEmptyTestString() {
        final GrokTesterResponse response = resource.grokTest("%{NUMBER}", "", false);
        assertThat(response.matched()).isFalse();
        assertThat(response.pattern()).isEqualTo("%{NUMBER}");
        assertThat(response.string()).isEqualTo("");
        assertThat(response.errorMessage()).isNullOrEmpty();
    }
}
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
package org.graylog.plugins.views.migrations;

import com.google.common.collect.ImmutableSet;
import org.graylog2.configuration.ElasticsearchConfiguration;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

class V20200730000000_AddGl2MessageIdFieldAliasForEventsTest {

    private ClusterConfigService clusterConfigService;
    private V20200730000000_AddGl2MessageIdFieldAliasForEvents.ElasticsearchAdapter elasticsearchAdapter;
    private ElasticsearchConfiguration elasticsearchConfig;
    private V20200730000000_AddGl2MessageIdFieldAliasForEvents sut;

    @BeforeEach
    void setUp() {
        clusterConfigService = mock(ClusterConfigService.class);
        elasticsearchAdapter = mock(V20200730000000_AddGl2MessageIdFieldAliasForEvents.ElasticsearchAdapter.class);
        elasticsearchConfig = mock(ElasticsearchConfiguration.class);
        mockConfiguredEventPrefixes("something", "something else");
        sut = new V20200730000000_AddGl2MessageIdFieldAliasForEvents(clusterConfigService, elasticsearchAdapter, elasticsearchConfig);
    }

    @Test
    void writesMigrationCompletedAfterSuccess() {
        final Set<String> modifiedIndices = ImmutableSet.of("some-index", "another-index");
        when(elasticsearchAdapter.addGl2MessageIdFieldAlias(any())).thenReturn(modifiedIndices);

        this.sut.upgrade();

        final V20200730000000_AddGl2MessageIdFieldAliasForEvents.MigrationCompleted migrationCompleted = captureMigrationCompleted();

        assertThat(migrationCompleted.modifiedIndices()).containsExactlyInAnyOrderElementsOf(modifiedIndices);
    }

    @Test
    void doesNotRunIfMigrationHasCompletedBefore() {
        when(clusterConfigService.get(V20200730000000_AddGl2MessageIdFieldAliasForEvents.MigrationCompleted.class))
                .thenReturn(V20200730000000_AddGl2MessageIdFieldAliasForEvents.MigrationCompleted.create(ImmutableSet.of()));

        this.sut.upgrade();

        verify(elasticsearchAdapter, never()).addGl2MessageIdFieldAlias(any());
    }

    @Test
    void usesEventIndexPrefixesFromElasticsearchConfig() {
        mockConfiguredEventPrefixes("events-prefix", "system-events-prefix");

        this.sut.upgrade();

        verify(elasticsearchAdapter)
                .addGl2MessageIdFieldAlias(ImmutableSet.of("events-prefix", "system-events-prefix"));
    }

    private void mockConfiguredEventPrefixes(String eventsPrefix, String systemEventsPrefix) {
        when(elasticsearchConfig.getDefaultEventsIndexPrefix()).thenReturn(eventsPrefix);
        when(elasticsearchConfig.getDefaultSystemEventsIndexPrefix()).thenReturn(systemEventsPrefix);
    }

    private V20200730000000_AddGl2MessageIdFieldAliasForEvents.MigrationCompleted captureMigrationCompleted() {
        final ArgumentCaptor<V20200730000000_AddGl2MessageIdFieldAliasForEvents.MigrationCompleted> migrationCompletedCaptor = ArgumentCaptor.forClass(V20200730000000_AddGl2MessageIdFieldAliasForEvents.MigrationCompleted.class);
        verify(clusterConfigService, times(1)).write(migrationCompletedCaptor.capture());
        return migrationCompletedCaptor.getValue();
    }
}

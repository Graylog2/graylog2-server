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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

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
        sut = buildSut("7");
    }

    private V20200730000000_AddGl2MessageIdFieldAliasForEvents buildSut(String version) {
        return new V20200730000000_AddGl2MessageIdFieldAliasForEvents(version, clusterConfigService, elasticsearchAdapter, elasticsearchConfig);
    }

    @Test
    void writesMigrationCompletedAfterSuccess() {
        mockConfiguredEventPrefixes("events-prefix", "system-events-prefix");

        this.sut.upgrade();

        final V20200730000000_AddGl2MessageIdFieldAliasForEvents.MigrationCompleted migrationCompleted = captureMigrationCompleted();

        assertThat(migrationCompleted.modifiedIndexPrefixes())
                .containsExactlyInAnyOrder("events-prefix", "system-events-prefix");
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

    @ParameterizedTest
    @ValueSource(strings = {"7", "8"})
    void runsForElasticsearchVersion7OrAbove(String version) {
        final V20200730000000_AddGl2MessageIdFieldAliasForEvents sut = buildSut(version);

        sut.upgrade();

        verify(elasticsearchAdapter).addGl2MessageIdFieldAlias(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"5", "6"})
    void doesNotRunForElasticsearchVersionBelow7(String version) {
        final V20200730000000_AddGl2MessageIdFieldAliasForEvents sut = buildSut(version);

        sut.upgrade();

        verify(elasticsearchAdapter, never()).addGl2MessageIdFieldAlias(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"5", "6"})
    void deletesMigrationCompletedMarkerForElasticsearchVersionBelow7(String version) {
        final V20200730000000_AddGl2MessageIdFieldAliasForEvents sut = buildSut(version);

        when(clusterConfigService.get(V20200730000000_AddGl2MessageIdFieldAliasForEvents.MigrationCompleted.class))
                .thenReturn(V20200730000000_AddGl2MessageIdFieldAliasForEvents.MigrationCompleted.create(ImmutableSet.of()));

        sut.upgrade();

        verify(clusterConfigService).remove(V20200730000000_AddGl2MessageIdFieldAliasForEvents.MigrationCompleted.class);
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

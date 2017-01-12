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
package org.graylog2.rest.resources.system.indexer;

import org.apache.shiro.subject.Subject;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.IndexSetRegistry;
import org.graylog2.indexer.IndexSetStatsCreator;
import org.graylog2.indexer.IndexSetValidator;
import org.graylog2.indexer.indexset.DefaultIndexSetConfig;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.indexer.indices.jobs.IndexSetCleanupJob;
import org.graylog2.indexer.retention.strategies.NoopRetentionStrategy;
import org.graylog2.indexer.retention.strategies.NoopRetentionStrategyConfig;
import org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategy;
import org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategyConfig;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.rest.resources.system.indexer.requests.IndexSetUpdateRequest;
import org.graylog2.rest.resources.system.indexer.responses.IndexSetResponse;
import org.graylog2.rest.resources.system.indexer.responses.IndexSetSummary;
import org.graylog2.shared.bindings.GuiceInjectorHolder;
import org.graylog2.system.jobs.SystemJobManager;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.inject.Provider;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotFoundException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class IndexSetsResourceTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();
    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Mock
    private IndexSetService indexSetService;
    @Mock
    private IndexSetRegistry indexSetRegistry;
    @Mock
    private IndexSetValidator indexSetValidator;
    @Mock
    private IndexSetCleanupJob.Factory indexSetCleanupJobFactory;
    @Mock
    private IndexSetStatsCreator indexSetStatsCreator;
    @Mock
    private SystemJobManager systemJobManager;
    @Mock
    private ClusterConfigService clusterConfigService;

    public IndexSetsResourceTest() {
        GuiceInjectorHolder.createInjector(Collections.emptyList());
    }

    private IndexSetsResource indexSetsResource;

    private Boolean permitted;

    @Before
    public void setUp() throws Exception {
        this.permitted = true;
        this.indexSetsResource = new TestResource(indexSetService, indexSetRegistry, indexSetValidator, indexSetCleanupJobFactory, indexSetStatsCreator, clusterConfigService, systemJobManager, () -> permitted);
    }

    private void notPermitted() {
        this.permitted = false;
    }

    @Test
    public void list() {
        final IndexSetConfig indexSetConfig = IndexSetConfig.create(
                "id",
                "title",
                "description",
                true,
                "prefix",
                1,
                0,
                MessageCountRotationStrategy.class.getCanonicalName(),
                MessageCountRotationStrategyConfig.create(1000),
                NoopRetentionStrategy.class.getCanonicalName(),
                NoopRetentionStrategyConfig.create(1),
                ZonedDateTime.of(2016, 10, 10, 12, 0, 0, 0, ZoneOffset.UTC),
                "standard",
                "index-template",
                1,
                false
        );
        when(indexSetService.findAll()).thenReturn(Collections.singletonList(indexSetConfig));

        final IndexSetResponse list = indexSetsResource.list(0, 0, false);

        verify(indexSetService, times(1)).findAll();
        verify(indexSetService, times(1)).getDefault();
        verifyNoMoreInteractions(indexSetService);

        assertThat(list.total()).isEqualTo(1);
        assertThat(list.indexSets()).containsExactly(IndexSetSummary.fromIndexSetConfig(indexSetConfig, false));
    }

    @Test
    public void listDenied() {
        notPermitted();

        final IndexSetConfig indexSetConfig = IndexSetConfig.create(
                "id",
                "title",
                "description",
                true,
                "prefix",
                1,
                0,
                MessageCountRotationStrategy.class.getCanonicalName(),
                MessageCountRotationStrategyConfig.create(1000),
                NoopRetentionStrategy.class.getCanonicalName(),
                NoopRetentionStrategyConfig.create(1),
                ZonedDateTime.of(2016, 10, 10, 12, 0, 0, 0, ZoneOffset.UTC),
                "standard",
                "index-template",
                1,
                false
        );
        when(indexSetService.findAll()).thenReturn(Collections.singletonList(indexSetConfig));

        final IndexSetResponse list = indexSetsResource.list(0, 0, false);

        verify(indexSetService, times(1)).findAll();
        verify(indexSetService, times(1)).getDefault();
        verifyNoMoreInteractions(indexSetService);

        assertThat(list.total()).isEqualTo(0);
        assertThat(list.indexSets()).isEmpty();
    }

    @Test
    public void list0() {
        when(indexSetService.findAll()).thenReturn(Collections.emptyList());

        final IndexSetResponse list = indexSetsResource.list(0, 0, false);

        verify(indexSetService, times(1)).findAll();
        verify(indexSetService, times(1)).getDefault();
        verifyNoMoreInteractions(indexSetService);

        assertThat(list.total()).isEqualTo(0);
        assertThat(list.indexSets()).isEmpty();
    }

    @Test
    public void get() {
        final IndexSetConfig indexSetConfig = IndexSetConfig.create(
                "id",
                "title",
                "description",
                true,
                "prefix",
                1,
                0,
                MessageCountRotationStrategy.class.getCanonicalName(),
                MessageCountRotationStrategyConfig.create(1000),
                NoopRetentionStrategy.class.getCanonicalName(),
                NoopRetentionStrategyConfig.create(1),
                ZonedDateTime.of(2016, 10, 10, 12, 0, 0, 0, ZoneOffset.UTC),
                "standard",
                "index-template",
                1,
                false
        );
        when(indexSetService.get("id")).thenReturn(Optional.of(indexSetConfig));

        final IndexSetSummary summary = indexSetsResource.get("id");

        verify(indexSetService, times(1)).get("id");
        verify(indexSetService, times(1)).getDefault();
        verifyNoMoreInteractions(indexSetService);
        assertThat(summary).isEqualTo(IndexSetSummary.fromIndexSetConfig(indexSetConfig, false));
    }

    @Test
    public void get0() {
        when(indexSetService.get("id")).thenReturn(Optional.empty());

        expectedException.expect(NotFoundException.class);
        expectedException.expectMessage("Couldn't load index set with ID <id>");

        try {
            indexSetsResource.get("id");
        } finally {
            verify(indexSetService, times(1)).get("id");
            verify(indexSetService, times(1)).getDefault();
            verifyNoMoreInteractions(indexSetService);
        }
    }

    @Test
    public void getDenied() {
        notPermitted();

        expectedException.expect(ForbiddenException.class);
        expectedException.expectMessage("Not authorized to access resource id <id>");

        try {
            indexSetsResource.get("id");
        } finally {
            verifyZeroInteractions(indexSetService);
        }
    }

    @Test
    public void save() {
        final IndexSetConfig indexSetConfig = IndexSetConfig.create(
                "title",
                "description",
                true,
                "prefix",
                1,
                0,
                MessageCountRotationStrategy.class.getCanonicalName(),
                MessageCountRotationStrategyConfig.create(1000),
                NoopRetentionStrategy.class.getCanonicalName(),
                NoopRetentionStrategyConfig.create(1),
                ZonedDateTime.of(2016, 10, 10, 12, 0, 0, 0, ZoneOffset.UTC),
                "standard",
                "prefix-template",
                1,
                false
        );
        final IndexSetConfig savedIndexSetConfig = indexSetConfig.toBuilder()
                .id("id")
                .build();

        when(indexSetService.save(indexSetConfig)).thenReturn(savedIndexSetConfig);

        final IndexSetSummary summary = indexSetsResource.save(IndexSetSummary.fromIndexSetConfig(indexSetConfig, false));

        verify(indexSetService, times(1)).save(indexSetConfig);
        verify(indexSetService, times(1)).getDefault();
        verifyNoMoreInteractions(indexSetService);
        assertThat(summary.toIndexSetConfig()).isEqualTo(savedIndexSetConfig);
    }

    @Test
    @Ignore("Currently doesn't work with @RequiresPermissions")
    public void saveDenied() {
        notPermitted();

        final IndexSetConfig indexSetConfig = IndexSetConfig.create(
                "title",
                "description",
                true,
                "prefix",
                1,
                0,
                MessageCountRotationStrategy.class.getCanonicalName(),
                MessageCountRotationStrategyConfig.create(1000),
                NoopRetentionStrategy.class.getCanonicalName(),
                NoopRetentionStrategyConfig.create(1),
                ZonedDateTime.of(2016, 10, 10, 12, 0, 0, 0, ZoneOffset.UTC),
                "standard",
                "index-template",
                1,
                false
        );

        expectedException.expect(ForbiddenException.class);
        expectedException.expectMessage("Not authorized to access resource id <id>");

        try {
            indexSetsResource.save(IndexSetSummary.fromIndexSetConfig(indexSetConfig, false));
        } finally {
            verifyZeroInteractions(indexSetService);
        }
    }

    @Test
    public void update() {
        final IndexSetConfig indexSetConfig = IndexSetConfig.create(
                "id",
                "new title",
                "description",
                true,
                "prefix",
                1,
                0,
                MessageCountRotationStrategy.class.getCanonicalName(),
                MessageCountRotationStrategyConfig.create(1000),
                NoopRetentionStrategy.class.getCanonicalName(),
                NoopRetentionStrategyConfig.create(1),
                ZonedDateTime.of(2016, 10, 10, 12, 0, 0, 0, ZoneOffset.UTC),
                "standard",
                "index-template",
                1,
                false
        );
        final IndexSetConfig updatedIndexSetConfig = indexSetConfig.toBuilder()
                .title("new title")
                .build();

        when(indexSetService.get("id")).thenReturn(Optional.of(indexSetConfig));
        when(indexSetService.save(indexSetConfig)).thenReturn(updatedIndexSetConfig);

        final IndexSetSummary summary = indexSetsResource.update("id", IndexSetUpdateRequest.fromIndexSetConfig(indexSetConfig));

        verify(indexSetService, times(1)).get("id");
        verify(indexSetService, times(1)).save(indexSetConfig);
        verify(indexSetService, times(1)).getDefault();
        verifyNoMoreInteractions(indexSetService);

        // The real update wouldn't replace the index template name…
        final IndexSetConfig actual = summary.toIndexSetConfig().toBuilder()
                .indexTemplateName("index-template")
                .build();
        assertThat(actual).isEqualTo(updatedIndexSetConfig);
    }

    @Test
    public void updateDenied() {
        notPermitted();
        final IndexSetConfig indexSetConfig = IndexSetConfig.create(
                "id",
                "title",
                "description",
                true,
                "prefix",
                1,
                0,
                MessageCountRotationStrategy.class.getCanonicalName(),
                MessageCountRotationStrategyConfig.create(1000),
                NoopRetentionStrategy.class.getCanonicalName(),
                NoopRetentionStrategyConfig.create(1),
                ZonedDateTime.of(2016, 10, 10, 12, 0, 0, 0, ZoneOffset.UTC),
                "standard",
                "index-template",
                1,
                false
        );

        expectedException.expect(ForbiddenException.class);
        expectedException.expectMessage("Not authorized to access resource id <wrong-id>");

        try {
            indexSetsResource.update("wrong-id", IndexSetUpdateRequest.fromIndexSetConfig(indexSetConfig));
        } finally {
            verifyZeroInteractions(indexSetService);
        }
    }

    @Test
    public void updateFailsWhenDefaultSetIsSetReadOnly() throws Exception {
        final String defaultIndexSetId = "defaultIndexSet";
        final IndexSetConfig defaultIndexSetConfig = IndexSetConfig.create(
            defaultIndexSetId,
            "title",
            "description",
            true,
            "prefix",
            1,
            0,
            MessageCountRotationStrategy.class.getCanonicalName(),
            MessageCountRotationStrategyConfig.create(1000),
            NoopRetentionStrategy.class.getCanonicalName(),
            NoopRetentionStrategyConfig.create(1),
            ZonedDateTime.of(2016, 10, 10, 12, 0, 0, 0, ZoneOffset.UTC),
            "standard",
            "index-template",
            1,
            false
        );

        when(indexSetService.getDefault()).thenReturn(defaultIndexSetConfig);
        when(indexSetService.get(defaultIndexSetId)).thenReturn(Optional.of(defaultIndexSetConfig));

        final IndexSetConfig defaultIndexSetConfigSetReadOnly = defaultIndexSetConfig.toBuilder().isWritable(false).build();

        expectedException.expect(ClientErrorException.class);
        expectedException.expectMessage("Default index set must be writable.");

        try {
            indexSetsResource.update("defaultIndexSet", IndexSetUpdateRequest.fromIndexSetConfig(defaultIndexSetConfigSetReadOnly));
        } finally {
            verify(indexSetService, never()).save(any());
        }
    }

    @Test
    public void delete() throws Exception {
        final IndexSet indexSet = mock(IndexSet.class);
        final IndexSetConfig indexSetConfig = mock(IndexSetConfig.class);

        when(indexSet.getConfig()).thenReturn(indexSetConfig);
        when(indexSetRegistry.get("id")).thenReturn(Optional.of(indexSet));
        when(indexSetCleanupJobFactory.create(indexSet)).thenReturn(mock(IndexSetCleanupJob.class));
        when(indexSetRegistry.getDefault()).thenReturn(null);
        when(indexSetService.delete("id")).thenReturn(1);

        indexSetsResource.delete("id", false);
        indexSetsResource.delete("id", true);

        verify(indexSetRegistry, times(2)).getDefault();
        verify(indexSetService, times(2)).delete("id");
        verify(systemJobManager, times(1)).submit(any(IndexSetCleanupJob.class));
        verifyNoMoreInteractions(indexSetService);
    }

    @Test
    public void delete0() throws Exception {
        final IndexSet indexSet = mock(IndexSet.class);
        final IndexSetConfig indexSetConfig = mock(IndexSetConfig.class);

        when(indexSet.getConfig()).thenReturn(indexSetConfig);
        when(indexSetRegistry.getDefault()).thenReturn(null);
        when(indexSetRegistry.get("id")).thenReturn(Optional.of(indexSet));
        when(indexSetService.delete("id")).thenReturn(0);

        expectedException.expect(NotFoundException.class);
        expectedException.expectMessage("Couldn't delete index set with ID <id>");

        try {
            indexSetsResource.delete("id", false);
        } finally {
            verify(indexSetRegistry, times(1)).getDefault();
            verify(indexSetService, times(1)).delete("id");
            verifyNoMoreInteractions(indexSetService);
        }
    }

    @Test
    public void deleteDefaultIndexSet() throws Exception {
        final IndexSet indexSet = mock(IndexSet.class);
        final IndexSetConfig indexSetConfig = mock(IndexSetConfig.class);

        when(indexSet.getConfig()).thenReturn(indexSetConfig);
        when(indexSetRegistry.getDefault()).thenReturn(indexSet);
        when(indexSetRegistry.get("id")).thenReturn(Optional.of(indexSet));
        when(indexSetCleanupJobFactory.create(indexSet)).thenReturn(mock(IndexSetCleanupJob.class));
        when(indexSetService.delete("id")).thenReturn(1);

        expectedException.expect(BadRequestException.class);

        indexSetsResource.delete("id", false);
        indexSetsResource.delete("id", true);

        verify(indexSetService, never()).delete("id");
        verify(systemJobManager, never()).submit(any(IndexSetCleanupJob.class));
        verifyNoMoreInteractions(indexSetService);
    }

    @Test
    public void deleteDenied() {
        notPermitted();

        expectedException.expect(ForbiddenException.class);
        expectedException.expectMessage("Not authorized to access resource id <id>");

        try {
            indexSetsResource.delete("id", false);
        } finally {
            verifyZeroInteractions(indexSetService);
        }
    }

    @Test
    public void setDefaultMakesIndexDefaultIfWritable() throws Exception {
        final String indexSetId = "newDefaultIndexSetId";
        final IndexSet indexSet = mock(IndexSet.class);
        final IndexSetConfig indexSetConfig = IndexSetConfig.create(
            indexSetId,
            "title",
            "description",
            true,
            "prefix",
            1,
            0,
            MessageCountRotationStrategy.class.getCanonicalName(),
            MessageCountRotationStrategyConfig.create(1000),
            NoopRetentionStrategy.class.getCanonicalName(),
            NoopRetentionStrategyConfig.create(1),
            ZonedDateTime.of(2016, 10, 10, 12, 0, 0, 0, ZoneOffset.UTC),
            "standard",
            "index-template",
            1,
            false
        );

        when(indexSet.getConfig()).thenReturn(indexSetConfig);
        when(indexSetService.get(indexSetId)).thenReturn(Optional.of(indexSetConfig));

        indexSetsResource.setDefault(indexSetId);

        final ArgumentCaptor<DefaultIndexSetConfig> defaultIndexSetIdCaptor = ArgumentCaptor.forClass(DefaultIndexSetConfig.class);
        verify(clusterConfigService, times(1)).write(defaultIndexSetIdCaptor.capture());

        final DefaultIndexSetConfig defaultIndexSetConfig = defaultIndexSetIdCaptor.getValue();
        assertThat(defaultIndexSetConfig).isNotNull();
        assertThat(defaultIndexSetConfig.defaultIndexSetId()).isEqualTo(indexSetId);
    }

    @Test
    public void setDefaultDoesNotDoAnyThingIfNotPermitted() throws Exception {
        notPermitted();

        expectedException.expect(ForbiddenException.class);
        expectedException.expectMessage("Not authorized to access resource id <someIndexSetId>");

        try {
            indexSetsResource.setDefault("someIndexSetId");
        } finally {
            verifyZeroInteractions(indexSetService);
            verifyZeroInteractions(clusterConfigService);
        }
    }

    @Test
    public void setDefaultDoesNotDoAnythingForInvalidId() throws Exception {
        final String nonExistingIndexSetId = "nonExistingId";

        when(indexSetService.get(nonExistingIndexSetId)).thenReturn(Optional.empty());

        expectedException.expect(NotFoundException.class);
        expectedException.expectMessage("Index set <" + nonExistingIndexSetId + "> does not exist");

        try {
            indexSetsResource.setDefault(nonExistingIndexSetId);
        } finally {
            verifyZeroInteractions(clusterConfigService);
        }
    }

    @Test
    public void setDefaultDoesNotDoAnythingIfIndexSetIsNotWritable() throws Exception {
        final String readOnlyIndexSetId = "newDefaultIndexSetId";
        final IndexSet readOnlyIndexSet = mock(IndexSet.class);
        final IndexSetConfig readOnlyIndexSetConfig = IndexSetConfig.create(
            readOnlyIndexSetId,
            "title",
            "description",
            false,
            "prefix",
            1,
            0,
            MessageCountRotationStrategy.class.getCanonicalName(),
            MessageCountRotationStrategyConfig.create(1000),
            NoopRetentionStrategy.class.getCanonicalName(),
            NoopRetentionStrategyConfig.create(1),
            ZonedDateTime.of(2016, 10, 10, 12, 0, 0, 0, ZoneOffset.UTC),
            "standard",
            "index-template",
            1,
            false
        );

        when(readOnlyIndexSet.getConfig()).thenReturn(readOnlyIndexSetConfig);
        when(indexSetService.get(readOnlyIndexSetId)).thenReturn(Optional.of(readOnlyIndexSetConfig));

        expectedException.expect(ClientErrorException.class);
        expectedException.expectMessage("Default index set must be writable.");

        try {
            indexSetsResource.setDefault(readOnlyIndexSetId);
        } finally {
            verifyZeroInteractions(clusterConfigService);
        }
    }

    private static class TestResource extends IndexSetsResource {
        private final Provider<Boolean> permitted;

        TestResource(IndexSetService indexSetService, IndexSetRegistry indexSetRegistry, IndexSetValidator indexSetValidator, IndexSetCleanupJob.Factory indexSetCleanupJobFactory, IndexSetStatsCreator indexSetStatsCreator, ClusterConfigService clusterConfigService, SystemJobManager systemJobManager, Provider<Boolean> permitted) {
            super(indexSetService, indexSetRegistry, indexSetValidator, indexSetCleanupJobFactory, indexSetStatsCreator, clusterConfigService, systemJobManager);
            this.permitted = permitted;
        }

        @Override
        protected Subject getSubject() {
            final Subject mockSubject = mock(Subject.class);
            when(mockSubject.isPermitted(anyString())).thenReturn(permitted.get());
            return mockSubject;
        }
    }
}

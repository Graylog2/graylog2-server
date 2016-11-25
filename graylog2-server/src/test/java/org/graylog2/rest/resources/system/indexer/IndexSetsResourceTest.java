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
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.indexer.retention.strategies.NoopRetentionStrategy;
import org.graylog2.indexer.retention.strategies.NoopRetentionStrategyConfig;
import org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategy;
import org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategyConfig;
import org.graylog2.rest.resources.system.indexer.responses.IndexSetResponse;
import org.graylog2.rest.resources.system.indexer.responses.IndexSetSummary;
import org.graylog2.shared.bindings.GuiceInjectorHolder;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotFoundException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
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

    public IndexSetsResourceTest() {
        GuiceInjectorHolder.createInjector(Collections.emptyList());
    }

    @Test
    public void list() {
        final IndexSetsResource indexSetsResource = new TestResource(indexSetService, true);
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
                ZonedDateTime.of(2016, 10, 10, 12, 0, 0, 0, ZoneOffset.UTC)
        );
        when(indexSetService.findAll()).thenReturn(Collections.singleton(indexSetConfig));

        final IndexSetResponse list = indexSetsResource.list();

        verify(indexSetService, times(1)).findAll();
        verifyNoMoreInteractions(indexSetService);

        assertThat(list.total()).isEqualTo(1);
        assertThat(list.indexSets()).containsExactly(IndexSetSummary.fromIndexSetConfig(indexSetConfig));
    }

    @Test
    public void listDenied() {
        final IndexSetsResource indexSetsResource = new TestResource(indexSetService, false);
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
                ZonedDateTime.of(2016, 10, 10, 12, 0, 0, 0, ZoneOffset.UTC)
        );
        when(indexSetService.findAll()).thenReturn(Collections.singleton(indexSetConfig));

        final IndexSetResponse list = indexSetsResource.list();

        verify(indexSetService, times(1)).findAll();
        verifyNoMoreInteractions(indexSetService);

        assertThat(list.total()).isEqualTo(0);
        assertThat(list.indexSets()).isEmpty();
    }

    @Test
    public void list0() {
        final IndexSetsResource indexSetsResource = new TestResource(indexSetService, true);
        when(indexSetService.findAll()).thenReturn(Collections.emptySet());

        final IndexSetResponse list = indexSetsResource.list();

        verify(indexSetService, times(1)).findAll();
        verifyNoMoreInteractions(indexSetService);

        assertThat(list.total()).isEqualTo(0);
        assertThat(list.indexSets()).isEmpty();
    }

    @Test
    public void get() {
        final IndexSetsResource indexSetsResource = new TestResource(indexSetService, true);
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
                ZonedDateTime.of(2016, 10, 10, 12, 0, 0, 0, ZoneOffset.UTC)
        );
        when(indexSetService.get("id")).thenReturn(Optional.of(indexSetConfig));

        final IndexSetSummary summary = indexSetsResource.get("id");

        verify(indexSetService, times(1)).get("id");
        verifyNoMoreInteractions(indexSetService);
        assertThat(summary).isEqualTo(IndexSetSummary.fromIndexSetConfig(indexSetConfig));
    }

    @Test
    public void get0() {
        final IndexSetsResource indexSetsResource = new TestResource(indexSetService, true);
        when(indexSetService.get("id")).thenReturn(Optional.empty());

        expectedException.expect(NotFoundException.class);
        expectedException.expectMessage("Couldn't load index set with ID <id>");

        try {
            indexSetsResource.get("id");
        } finally {
            verify(indexSetService, times(1)).get("id");
            verifyNoMoreInteractions(indexSetService);
        }
    }

    @Test
    public void getDenied() {
        final IndexSetsResource indexSetsResource = new TestResource(indexSetService, false);

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
        final IndexSetsResource indexSetsResource = new TestResource(indexSetService, true);
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
                ZonedDateTime.of(2016, 10, 10, 12, 0, 0, 0, ZoneOffset.UTC)
        );
        final IndexSetConfig savedIndexSetConfig = indexSetConfig.toBuilder()
                .id("id")
                .build();

        when(indexSetService.save(indexSetConfig)).thenReturn(savedIndexSetConfig);

        final IndexSetSummary summary = indexSetsResource.save(IndexSetSummary.fromIndexSetConfig(indexSetConfig));

        verify(indexSetService, times(1)).save(indexSetConfig);
        verifyNoMoreInteractions(indexSetService);
        assertThat(summary.toIndexSetConfig()).isEqualTo(savedIndexSetConfig);
    }

    @Test
    @Ignore("Currently doesn't work with @RequiresPermissions")
    public void saveDenied() {
        final IndexSetsResource indexSetsResource = new TestResource(indexSetService, false);
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
                ZonedDateTime.of(2016, 10, 10, 12, 0, 0, 0, ZoneOffset.UTC)
        );

        expectedException.expect(ForbiddenException.class);
        expectedException.expectMessage("Not authorized to access resource id <id>");

        try {
            indexSetsResource.save(IndexSetSummary.fromIndexSetConfig(indexSetConfig));
        } finally {
            verifyZeroInteractions(indexSetService);
        }
    }

    @Test
    public void update() {
        final IndexSetsResource indexSetsResource = new TestResource(indexSetService, true);
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
                ZonedDateTime.of(2016, 10, 10, 12, 0, 0, 0, ZoneOffset.UTC)
        );
        final IndexSetConfig updatedIndexSetConfig = indexSetConfig.toBuilder()
                .title("new title")
                .build();

        when(indexSetService.save(indexSetConfig)).thenReturn(updatedIndexSetConfig);

        final IndexSetSummary summary = indexSetsResource.update("id", IndexSetSummary.fromIndexSetConfig(indexSetConfig));

        verify(indexSetService, times(1)).save(indexSetConfig);
        verifyNoMoreInteractions(indexSetService);
        assertThat(summary.toIndexSetConfig()).isEqualTo(updatedIndexSetConfig);
    }

    @Test
    public void updateIdMismatch() {
        final IndexSetsResource indexSetsResource = new TestResource(indexSetService, true);
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
                ZonedDateTime.of(2016, 10, 10, 12, 0, 0, 0, ZoneOffset.UTC)
        );

        expectedException.expect(ClientErrorException.class);
        expectedException.expectMessage("Mismatch of IDs in URI path and payload");

        try {
            indexSetsResource.update("wrong-id", IndexSetSummary.fromIndexSetConfig(indexSetConfig));
        } finally {
            verifyZeroInteractions(indexSetService);
        }
    }

    @Test
    public void updateDenied() {
        final IndexSetsResource indexSetsResource = new TestResource(indexSetService, false);
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
                ZonedDateTime.of(2016, 10, 10, 12, 0, 0, 0, ZoneOffset.UTC)
        );

        expectedException.expect(ForbiddenException.class);
        expectedException.expectMessage("Not authorized to access resource id <wrong-id>");

        try {
            indexSetsResource.update("wrong-id", IndexSetSummary.fromIndexSetConfig(indexSetConfig));
        } finally {
            verifyZeroInteractions(indexSetService);
        }
    }

    @Test
    public void delete() {
        final IndexSetsResource indexSetsResource = new TestResource(indexSetService, true);
        when(indexSetService.delete("id")).thenReturn(1);

        indexSetsResource.delete("id");

        verify(indexSetService, times(1)).delete("id");
        verifyNoMoreInteractions(indexSetService);
    }

    @Test
    public void delete0() {
        final IndexSetsResource indexSetsResource = new TestResource(indexSetService, true);
        when(indexSetService.delete("id")).thenReturn(0);

        expectedException.expect(NotFoundException.class);
        expectedException.expectMessage("Couldn't delete index set with ID <id>");

        try {
            indexSetsResource.delete("id");
        } finally {
            verify(indexSetService, times(1)).delete("id");
            verifyNoMoreInteractions(indexSetService);
        }
    }

    @Test
    public void deleteDenied() {
        final IndexSetsResource indexSetsResource = new TestResource(indexSetService, false);

        expectedException.expect(ForbiddenException.class);
        expectedException.expectMessage("Not authorized to access resource id <id>");

        try {
            indexSetsResource.delete("id");
        } finally {
            verifyZeroInteractions(indexSetService);
        }
    }

    private static class TestResource extends IndexSetsResource {
        private final boolean permitted;

        TestResource(IndexSetService indexSetService, boolean permitted) {
            super(indexSetService);
            this.permitted = permitted;
        }

        @Override
        protected Subject getSubject() {
            final Subject mockSubject = mock(Subject.class);
            when(mockSubject.isPermitted(anyString())).thenReturn(permitted);
            return mockSubject;
        }
    }
}
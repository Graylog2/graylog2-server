/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.rest.resources.system.indexer;

import jakarta.validation.Validator;
import jakarta.ws.rs.ClientErrorException;
import org.apache.shiro.subject.Subject;
import org.graylog2.cluster.lock.AlreadyLockedException;
import org.graylog2.cluster.lock.RefreshingLockService;
import org.graylog2.datatiering.DataTieringConfig;
import org.graylog2.indexer.indexset.template.IndexSetDefaultTemplateService;
import org.graylog2.indexer.indexset.template.IndexSetTemplate;
import org.graylog2.indexer.indexset.template.IndexSetTemplateConfig;
import org.graylog2.indexer.indexset.template.IndexSetTemplateRequest;
import org.graylog2.indexer.indexset.template.IndexSetTemplateService;
import org.graylog2.indexer.indexset.template.requirement.IndexSetTemplateRequirement;
import org.graylog2.indexer.indexset.template.requirement.IndexSetTemplateRequirementsChecker;
import org.graylog2.indexer.indexset.validation.IndexSetValidator;
import org.graylog2.shared.bindings.GuiceInjectorHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class IndexSetTemplateResourceTest {

    @Mock
    private IndexSetValidator indexSetValidator;
    @Mock
    private Validator validator;
    @Mock
    private IndexSetTemplateService templateService;
    @Mock
    private IndexSetDefaultTemplateService indexSetDefaultTemplateService;
    @Mock
    private IndexSetTemplateRequirementsChecker indexSetTemplateRequirementsChecker;
    @Mock
    private RefreshingLockService.Factory lockServiceFactory;
    @Mock
    private RefreshingLockService lockService;
    @Mock
    private IndexSetTemplateConfig indexSetConfig;

    private IndexSetTemplateResource resource;

    public IndexSetTemplateResourceTest() {
        GuiceInjectorHolder.createInjector(Collections.emptyList());
    }

    @BeforeEach
    public void setUp() {
        resource = new TestResource(indexSetValidator, validator, templateService, indexSetDefaultTemplateService,
                indexSetTemplateRequirementsChecker, lockServiceFactory);
    }

    @Test
    public void createWithoutWarmTierRepositoryTakesNoLock() {
        when(indexSetConfig.useLegacyRotation()).thenReturn(false);
        when(templateService.save(any())).thenReturn(template());
        when(indexSetTemplateRequirementsChecker.check(any())).thenReturn(new IndexSetTemplateRequirement.Result(true, ""));

        resource.create(new IndexSetTemplateRequest("title", "description", indexSetConfig));

        verify(templateService).save(any());
        verifyNoInteractions(lockServiceFactory);
    }

    @Test
    public void createHoldsRepositoryLockThroughValidationAndPersistence() throws Exception {
        final DataTieringConfig dataTieringConfig = dataTieringWithLockId();
        when(indexSetConfig.useLegacyRotation()).thenReturn(false);
        when(indexSetConfig.dataTieringConfig()).thenReturn(dataTieringConfig);
        when(lockServiceFactory.create()).thenReturn(lockService);
        when(templateService.save(any())).thenReturn(template());
        when(indexSetTemplateRequirementsChecker.check(any())).thenReturn(new IndexSetTemplateRequirement.Result(true, ""));

        resource.create(new IndexSetTemplateRequest("title", "description", indexSetConfig));

        final InOrder inOrder = inOrder(lockService, templateService);
        inOrder.verify(lockService).acquireAndKeepLock("warm-tier-repository/repo1", 1);
        inOrder.verify(templateService).save(any());
        inOrder.verify(lockService).close();
    }

    @Test
    public void updateReturns409WithoutPersistenceOnLockContention() throws Exception {
        final DataTieringConfig dataTieringConfig = dataTieringWithLockId();
        when(templateService.get("template-id")).thenReturn(Optional.of(template()));
        when(indexSetTemplateRequirementsChecker.check(any())).thenReturn(new IndexSetTemplateRequirement.Result(true, ""));
        when(indexSetConfig.dataTieringConfig()).thenReturn(dataTieringConfig);
        when(lockServiceFactory.create()).thenReturn(lockService);
        doThrow(new AlreadyLockedException("locked")).when(lockService).acquireAndKeepLock("warm-tier-repository/repo1", 1);

        final ClientErrorException exception = assertThrows(ClientErrorException.class,
                () -> resource.update("template-id", new IndexSetTemplateRequest("title", "description", indexSetConfig)));

        assertThat(exception.getResponse().getStatus()).isEqualTo(409);
        verify(templateService, never()).update(any(), any());
    }

    private static DataTieringConfig dataTieringWithLockId() {
        final DataTieringConfig dataTieringConfig = mock(DataTieringConfig.class);
        when(dataTieringConfig.repositoryLockId()).thenReturn(Optional.of("warm-tier-repository/repo1"));
        return dataTieringConfig;
    }

    private IndexSetTemplate template() {
        return new IndexSetTemplate("template-id", "title", "description", false, indexSetConfig);
    }

    private static class TestResource extends IndexSetTemplateResource {

        TestResource(IndexSetValidator indexSetValidator, Validator validator, IndexSetTemplateService templateService,
                     IndexSetDefaultTemplateService indexSetDefaultTemplateService,
                     IndexSetTemplateRequirementsChecker indexSetTemplateRequirementsChecker,
                     RefreshingLockService.Factory lockServiceFactory) {
            super(indexSetValidator, validator, templateService, indexSetDefaultTemplateService,
                    indexSetTemplateRequirementsChecker, lockServiceFactory);
        }

        @Override
        protected Subject getSubject() {
            final Subject mockSubject = mock(Subject.class);
            lenient().when(mockSubject.isPermitted(anyString())).thenReturn(true);
            lenient().when(mockSubject.getPrincipal()).thenReturn("test-user");
            return mockSubject;
        }
    }
}

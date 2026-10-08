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
package org.graylog2.rest.resources.system.jobs;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import org.apache.shiro.subject.Subject;
import org.graylog.scheduler.rest.JobResourceHandlerService;
import org.graylog.scheduler.system.SystemJobManager;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.rest.models.system.SystemJobSummary;
import org.graylog2.shared.bindings.GuiceInjectorHolder;
import org.graylog2.system.jobs.LegacySystemJobFactory;
import org.graylog2.system.jobs.LegacySystemJobManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Guards the regression fixed in #27080: {@code cancel} on a system-scheduler job must reach {@link SystemJobManager}
 * rather than 404 on the legacy-only path. The pre-fix resource consulted only {@link LegacySystemJobManager}.
 */
@ExtendWith(MockitoExtension.class)
class SystemJobResourceTest {
    private static final String SCHEDULER_JOB_ID = "6890abcdef0123456789abcd";
    private static final String HANDLER_MANAGED_JOB_TYPE = "archive-create-execution-v1";

    @Mock
    private LegacySystemJobFactory legacySystemJobFactory;
    @Mock
    private LegacySystemJobManager legacySystemJobManager;
    @Mock
    private SystemJobManager systemJobManager;
    @Mock
    private NodeId nodeId;
    @Mock
    private JobResourceHandlerService jobResourceHandlerService;

    // Allow all permissions by default; individual tests flip this to deny.
    private Predicate<String> permissionCheck = permission -> true;

    private Subject subject;
    private SystemJobResource resource;

    @BeforeEach
    void setUp() {
        // RestResource relies on statically-provided fields; an injector must be present during construction.
        GuiceInjectorHolder.createInjector(Collections.emptyList());
        // Lenient: not every test reaches a permission check, and a denied check reads getPrincipal() for its log.
        subject = mock(Subject.class);
        lenient().when(subject.isPermitted(anyString()))
                .thenAnswer(invocation -> permissionCheck.test(invocation.getArgument(0)));
        lenient().when(subject.getPrincipal()).thenReturn("test-user");
        resource = new TestResource();
    }

    @Test
    void cancelReachesSchedulerForCancelableJob() {
        when(legacySystemJobManager.getRunningJobs()).thenReturn(Collections.emptyMap());
        final SystemJobSummary summary = schedulerSummary(true);
        when(systemJobManager.getRunningJob(SCHEDULER_JOB_ID)).thenReturn(Optional.of(summary));

        final SystemJobSummary result = resource.cancel(SCHEDULER_JOB_ID);

        assertThat(result).isEqualTo(summary);
        verify(systemJobManager).cancel(SCHEDULER_JOB_ID);
    }

    @Test
    void cancelRejectsNonCancelableSchedulerJob() {
        when(legacySystemJobManager.getRunningJobs()).thenReturn(Collections.emptyMap());
        when(systemJobManager.getRunningJob(SCHEDULER_JOB_ID)).thenReturn(Optional.of(schedulerSummary(false)));

        assertThatThrownBy(() -> resource.cancel(SCHEDULER_JOB_ID)).isInstanceOf(ForbiddenException.class);

        verify(systemJobManager, never()).cancel(anyString());
    }

    @Test
    void cancelReturnsNotFoundWhenNoSuchJob() {
        when(legacySystemJobManager.getRunningJobs()).thenReturn(Collections.emptyMap());
        when(systemJobManager.getRunningJob(SCHEDULER_JOB_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resource.cancel(SCHEDULER_JOB_ID)).isInstanceOf(NotFoundException.class);

        verify(systemJobManager, never()).cancel(anyString());
    }

    @Test
    void cancelLeavesHandlerManagedJobToItsOwnHandler() {
        // A job type with its own JobResourceHandler must not be cancelled here: the handler applies the plugin's own
        // permission checks, and ClusterSystemJobResource only falls through to it when every node reports not-found.
        when(legacySystemJobManager.getRunningJobs()).thenReturn(Collections.emptyMap());
        when(systemJobManager.getRunningJob(SCHEDULER_JOB_ID)).thenReturn(Optional.of(handlerManagedSummary()));
        when(jobResourceHandlerService.handlesJobType(HANDLER_MANAGED_JOB_TYPE)).thenReturn(true);

        assertThatThrownBy(() -> resource.cancel(SCHEDULER_JOB_ID)).isInstanceOf(NotFoundException.class);

        verify(systemJobManager, never()).cancel(anyString());
    }

    @Test
    void cancelChecksDeletePermissionOnSchedulerJob() {
        permissionCheck = permission -> false;
        when(legacySystemJobManager.getRunningJobs()).thenReturn(Collections.emptyMap());
        when(systemJobManager.getRunningJob(SCHEDULER_JOB_ID)).thenReturn(Optional.of(schedulerSummary(true)));

        assertThatThrownBy(() -> resource.cancel(SCHEDULER_JOB_ID)).isInstanceOf(ForbiddenException.class);

        verify(systemJobManager, never()).cancel(anyString());
    }

    private static SystemJobSummary handlerManagedSummary() {
        return SystemJobSummary.create(SCHEDULER_JOB_ID, "Archive create", HANDLER_MANAGED_JOB_TYPE, "",
                "node-1", null, 0, true, true);
    }

    private static SystemJobSummary schedulerSummary(boolean cancelable) {
        // name() is the job type used for the permission check.
        return SystemJobSummary.create(SCHEDULER_JOB_ID, "Rebuild index ranges", "rebuild-index-ranges", "",
                "node-1", null, 0, cancelable, true);
    }

    private class TestResource extends SystemJobResource {
        TestResource() {
            super(legacySystemJobFactory, legacySystemJobManager, systemJobManager, nodeId, jobResourceHandlerService);
        }

        @Override
        protected Subject getSubject() {
            return subject;
        }
    }
}

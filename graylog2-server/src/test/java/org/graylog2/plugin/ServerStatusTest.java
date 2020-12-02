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
package org.graylog2.plugin;

import com.google.common.eventbus.EventBus;
import org.graylog2.audit.NullAuditEventSender;
import org.graylog2.plugin.lifecycles.Lifecycle;
import org.graylog2.shared.SuppressForbidden;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ServerStatusTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock private BaseConfiguration config;
    @Mock private EventBus eventBus;

    private ServerStatus status;
    private File tempFile;

    @Before
    public void setUp() throws Exception {
        tempFile = temporaryFolder.newFile();

        when(config.getNodeIdFile()).thenReturn(tempFile.getPath());

        status = new ServerStatus(config, Collections.singleton(ServerStatus.Capability.MASTER), eventBus, NullAuditEventSender::new);
    }

    @Test
    public void testGetNodeId() throws Exception {
        assertEquals(new String(Files.readAllBytes(tempFile.toPath()), StandardCharsets.UTF_8), status.getNodeId().toString());
    }

    @Test
    public void testGetLifecycle() throws Exception {
        assertEquals(Lifecycle.UNINITIALIZED, status.getLifecycle());
    }

    @Test
    public void testSetLifecycleRunning() throws Exception {
        status.start();
        assertTrue(status.isProcessing());
        verify(eventBus).post(Lifecycle.RUNNING);
    }

    @Test
    public void testSetLifecycleUninitialized() throws Exception {
        assertFalse(status.isProcessing());
        verify(eventBus, never()).post(Lifecycle.UNINITIALIZED);
    }

    @Test
    public void testSetLifecycleStarting() throws Exception {
        status.initialize();
        assertFalse(status.isProcessing());
        verify(eventBus).post(Lifecycle.STARTING);
    }

    @Test
    public void testSetLifecyclePaused() throws Exception {
        status.pauseMessageProcessing(false);
        assertFalse(status.isProcessing());
        verify(eventBus).post(Lifecycle.PAUSED);
    }

    @Test
    public void testAwaitRunning() throws Exception {
        final Runnable runnable = mock(Runnable.class);
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch stopLatch = new CountDownLatch(1);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    startLatch.countDown();
                    status.awaitRunning(runnable);
                } finally {
                    stopLatch.countDown();
                }
            }
        }).start();

        startLatch.await(5, TimeUnit.SECONDS);
        verify(runnable, never()).run();

        status.start();

        stopLatch.await(5, TimeUnit.SECONDS);
        verify(runnable).run();
    }

    @Test
    public void testAwaitRunningWithException() throws Exception {
        final Runnable runnable = mock(Runnable.class);
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch stopLatch = new CountDownLatch(1);
        final AtomicBoolean exceptionCaught = new AtomicBoolean(false);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    startLatch.countDown();
                    status.awaitRunning(runnable);
                    exceptionCaught.set(true);
                } finally {
                    stopLatch.countDown();
                }
            }
        }).start();

        startLatch.await(5, TimeUnit.SECONDS);
        status.start();
        stopLatch.await(5, TimeUnit.SECONDS);

        assertTrue(exceptionCaught.get());
    }

    @Test
    public void testGetStartedAt() throws Exception {
        assertNotNull(status.getStartedAt());
    }

    @Test
    @SuppressForbidden("Deliberate invocation")
    public void testGetTimezone() throws Exception {
        assertEquals(DateTimeZone.getDefault(), status.getTimezone());
    }

    @Test
    public void testAddCapability() throws Exception {
        assertEquals(status, status.addCapability(ServerStatus.Capability.SERVER));
        assertTrue(status.hasCapabilities(ServerStatus.Capability.MASTER, ServerStatus.Capability.SERVER));
    }

    @Test
    public void testAddCapabilities() throws Exception {
        assertEquals(status, status.addCapabilities(ServerStatus.Capability.LOCALMODE));
        assertTrue(status.hasCapabilities(ServerStatus.Capability.MASTER, ServerStatus.Capability.LOCALMODE));
    }

    @Test
    public void testPauseMessageProcessing() throws Exception {
        status.pauseMessageProcessing();

        assertEquals(Lifecycle.PAUSED, status.getLifecycle());
        assertTrue(status.processingPauseLocked());
    }

    @Test
    public void testPauseMessageProcessingWithLock() throws Exception {
        status.pauseMessageProcessing(true);

        assertEquals(Lifecycle.PAUSED, status.getLifecycle());
        assertTrue(status.processingPauseLocked());
    }

    @Test
    public void testPauseMessageProcessingWithoutLock() throws Exception {
        status.pauseMessageProcessing(false);

        assertEquals(Lifecycle.PAUSED, status.getLifecycle());
        assertFalse(status.processingPauseLocked());
    }

    @Test
    public void testPauseMessageProcessingNotOverridingLock() throws Exception {
        status.pauseMessageProcessing(true);
        status.pauseMessageProcessing(false);

        assertTrue(status.processingPauseLocked());
    }

    @Test
    public void testResumeMessageProcessingWithoutLock() throws Exception {
        status.pauseMessageProcessing(false);
        status.resumeMessageProcessing();

        assertEquals(Lifecycle.RUNNING, status.getLifecycle());
    }

    @Test(expected = ProcessingPauseLockedException.class)
    public void testResumeMessageProcessingWithLock() throws Exception {
        status.pauseMessageProcessing(true);
        status.resumeMessageProcessing();
    }

    @Test
    public void testUnlockProcessingPause() throws Exception {
        status.pauseMessageProcessing(true);
        assertTrue(status.processingPauseLocked());

        status.unlockProcessingPause();
        assertFalse(status.processingPauseLocked());
    }

    @Test
    public void testSetLocalMode() throws Exception {
        status.setLocalMode(false);
        assertFalse(status.hasCapability(ServerStatus.Capability.LOCALMODE));

        status.setLocalMode(true);
        assertTrue(status.hasCapability(ServerStatus.Capability.LOCALMODE));
    }
}

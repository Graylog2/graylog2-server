/*
 * Copyright 2012-2014 TORCH GmbH
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.graylog2.shared;

import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;
import org.graylog2.Configuration;
import org.graylog2.plugin.lifecycles.Lifecycle;
import org.joda.time.DateTimeZone;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.nio.file.Files;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.*;

public class ServerStatusTest {
    @Mock private Configuration config;
    @Mock private EventBus eventBus;

    private ServerStatus status;
    private File tempFile;

    @BeforeMethod
    public void setUp() throws Exception {
        tempFile = File.createTempFile("server-status-test", "node-id");
        tempFile.deleteOnExit();

        MockitoAnnotations.initMocks(this);

        when(config.getNodeIdFile()).thenReturn(tempFile.getPath());

        status = new ServerStatus(config, Sets.newHashSet(ServerStatus.Capability.MASTER), eventBus);
    }

    @Test
    public void testGetNodeId() throws Exception {
        assertEquals(status.getNodeId().toString(), new String(Files.readAllBytes(tempFile.toPath())));
    }

    @Test
    public void testGetLifecycle() throws Exception {
        assertEquals(status.getLifecycle(), Lifecycle.UNINITIALIZED);
    }

    @Test
    public void testSetLifecycleRunning() throws Exception {
        status.setLifecycle(Lifecycle.RUNNING);
        assertTrue(status.isProcessing());
        verify(eventBus).post(Lifecycle.RUNNING);
    }

    @Test
    public void testSetLifecycleUninitialized() throws Exception {
        status.setLifecycle(Lifecycle.UNINITIALIZED);
        assertFalse(status.isProcessing());
        verify(eventBus, times(2)).post(Lifecycle.UNINITIALIZED);
    }

    @Test
    public void testSetLifecycleStarting() throws Exception {
        status.setLifecycle(Lifecycle.STARTING);
        assertFalse(status.isProcessing());
        verify(eventBus).post(Lifecycle.STARTING);
    }

    @Test
    public void testSetLifecyclePaused() throws Exception {
        status.setLifecycle(Lifecycle.PAUSED);
        assertFalse(status.isProcessing());
        verify(eventBus).post(Lifecycle.PAUSED);
    }

    @Test
    public void testGetStartedAt() throws Exception {
        assertNotNull(status.getStartedAt());
    }

    @Test
    public void testGetTimezone() throws Exception {
        assertEquals(status.getTimezone(), DateTimeZone.getDefault());
    }

    @Test
    public void testAddCapability() throws Exception {
        assertEquals(status.addCapability(ServerStatus.Capability.SERVER), status);
        assertTrue(status.hasCapabilities(ServerStatus.Capability.MASTER, ServerStatus.Capability.SERVER));
    }

    @Test
    public void testAddCapabilities() throws Exception {
        assertEquals(status.addCapabilities(ServerStatus.Capability.LOCALMODE, ServerStatus.Capability.STATSMODE), status);
        assertTrue(status.hasCapabilities(ServerStatus.Capability.MASTER, ServerStatus.Capability.LOCALMODE, ServerStatus.Capability.STATSMODE));
    }

    @Test
    public void testPauseMessageProcessing() throws Exception {
        status.pauseMessageProcessing();

        assertEquals(status.getLifecycle(), Lifecycle.PAUSED);
        assertTrue(status.processingPauseLocked());
    }

    @Test
    public void testPauseMessageProcessingWithLock() throws Exception {
        status.pauseMessageProcessing(true);

        assertEquals(status.getLifecycle(), Lifecycle.PAUSED);
        assertTrue(status.processingPauseLocked());
    }

    @Test
    public void testPauseMessageProcessingWithoutLock() throws Exception {
        status.pauseMessageProcessing(false);

        assertEquals(status.getLifecycle(), Lifecycle.PAUSED);
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

        assertEquals(status.getLifecycle(), Lifecycle.RUNNING);
    }

    @Test(expectedExceptions = ProcessingPauseLockedException.class)
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
    public void testSetStatsMode() throws Exception {
        status.setStatsMode(false);
        assertFalse(status.hasCapability(ServerStatus.Capability.STATSMODE));

        status.setStatsMode(true);
        assertTrue(status.hasCapability(ServerStatus.Capability.STATSMODE));
    }

    @Test
    public void testSetLocalMode() throws Exception {
        status.setLocalMode(false);
        assertFalse(status.hasCapability(ServerStatus.Capability.LOCALMODE));

        status.setLocalMode(true);
        assertTrue(status.hasCapability(ServerStatus.Capability.LOCALMODE));
    }
}
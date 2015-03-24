/**
 * The MIT License
 * Copyright (c) 2012 Graylog, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.graylog2.plugin;

import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;
import org.graylog2.plugin.lifecycles.Lifecycle;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ServerStatusTest {
    @Mock private BaseConfiguration config;
    @Mock private EventBus eventBus;

    private ServerStatus status;
    private File tempFile;

    @Before
    public void setUp() throws Exception {
        tempFile = File.createTempFile("server-status-test", "node-id");
        tempFile.deleteOnExit();

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

        doThrow(new RuntimeException()).when(runnable).run();

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
        assertEquals(status.addCapabilities(ServerStatus.Capability.LOCALMODE), status);
        assertTrue(status.hasCapabilities(ServerStatus.Capability.MASTER, ServerStatus.Capability.LOCALMODE));
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

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
package org.graylog2.plugin.inputs.transports;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.MoreExecutors;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.util.ConnectionCounter;
import org.graylog2.plugin.inputs.util.ThroughputCounter;
import org.jboss.netty.bootstrap.Bootstrap;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.util.HashedWheelTimer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.mock;

public class AbstractTcpTransportTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public final RestoreSystemProperties restoreSystemProperties = new RestoreSystemProperties();

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Mock
    private MessageInput input;

    private ThroughputCounter throughputCounter;
    private LocalMetricRegistry localRegistry;
    private Executor bossPool;
    private Executor workerPool;
    private ConnectionCounter connectionCounter;

    @Before
    public void setUp() {
        throughputCounter = new ThroughputCounter(new HashedWheelTimer());
        localRegistry = new LocalMetricRegistry();
        bossPool = MoreExecutors.directExecutor();
        workerPool = MoreExecutors.directExecutor();
        connectionCounter = new ConnectionCounter();
    }

    @Test
    public void getBaseChannelHandlersGeneratesSelfSignedCertificates() {
        final Configuration configuration = new Configuration(ImmutableMap.of(
            "bind_address", "localhost",
            "port", 12345,
            "tls_enable", true)
        );

        final AbstractTcpTransport transport = new AbstractTcpTransport(
            configuration, throughputCounter, localRegistry, bossPool, workerPool, connectionCounter) {
            @Override
            protected Bootstrap getBootstrap() {
                return super.getBootstrap();
            }

            @Override
            protected LinkedHashMap<String, Callable<? extends ChannelHandler>> getBaseChannelHandlers(MessageInput input) {
                return super.getBaseChannelHandlers(input);
            }
        };
        final MessageInput input = mock(MessageInput.class);
        assertThat(transport.getBaseChannelHandlers(input)).containsKey("tls");
    }

    @Test
    public void getBaseChannelHandlersFailsIfTempDirDoesNotExist() throws IOException {
        final File tmpDir = temporaryFolder.newFolder();
        assumeTrue(tmpDir.delete());
        System.setProperty("java.io.tmpdir", tmpDir.getAbsolutePath());

        final Configuration configuration = new Configuration(ImmutableMap.of(
            "bind_address", "localhost",
            "port", 12345,
            "tls_enable", true)
        );

        final AbstractTcpTransport transport = new AbstractTcpTransport(
            configuration, throughputCounter, localRegistry, bossPool, workerPool, connectionCounter) {
            @Override
            protected Bootstrap getBootstrap() {
                return super.getBootstrap();
            }

            @Override
            protected LinkedHashMap<String, Callable<? extends ChannelHandler>> getBaseChannelHandlers(MessageInput input) {
                return super.getBaseChannelHandlers(input);
            }
        };

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("Couldn't write to temporary directory: " + tmpDir.getAbsolutePath());

        transport.getBaseChannelHandlers(input);
    }

    @Test
    public void getBaseChannelHandlersFailsIfTempDirIsNotWritable() throws IOException {
        final File tmpDir = temporaryFolder.newFolder();
        assumeTrue(tmpDir.setWritable(false));
        assumeFalse(tmpDir.canWrite());
        System.setProperty("java.io.tmpdir", tmpDir.getAbsolutePath());

        final Configuration configuration = new Configuration(ImmutableMap.of(
            "bind_address", "localhost",
            "port", 12345,
            "tls_enable", true)
        );

        final AbstractTcpTransport transport = new AbstractTcpTransport(
            configuration, throughputCounter, localRegistry, bossPool, workerPool, connectionCounter) {
            @Override
            protected Bootstrap getBootstrap() {
                return super.getBootstrap();
            }

            @Override
            protected LinkedHashMap<String, Callable<? extends ChannelHandler>> getBaseChannelHandlers(MessageInput input) {
                return super.getBaseChannelHandlers(input);
            }
        };

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("Couldn't write to temporary directory: " + tmpDir.getAbsolutePath());

        transport.getBaseChannelHandlers(input);
    }

    @Test
    public void getBaseChannelHandlersFailsIfTempDirIsNoDirectory() throws IOException {
        final File file = temporaryFolder.newFile();
        assumeTrue(file.isFile());
        System.setProperty("java.io.tmpdir", file.getAbsolutePath());

        final Configuration configuration = new Configuration(ImmutableMap.of(
            "bind_address", "localhost",
            "port", 12345,
            "tls_enable", true)
        );

        final AbstractTcpTransport transport = new AbstractTcpTransport(
            configuration, throughputCounter, localRegistry, bossPool, workerPool, connectionCounter) {
            @Override
            protected Bootstrap getBootstrap() {
                return super.getBootstrap();
            }

            @Override
            protected LinkedHashMap<String, Callable<? extends ChannelHandler>> getBaseChannelHandlers(MessageInput input) {
                return super.getBaseChannelHandlers(input);
            }
        };

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("Couldn't write to temporary directory: " + file.getAbsolutePath());

        transport.getBaseChannelHandlers(input);
    }
}

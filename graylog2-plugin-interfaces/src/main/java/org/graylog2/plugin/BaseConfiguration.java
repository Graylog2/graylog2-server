/**
 * The MIT License
 * Copyright (c) 2012 TORCH GmbH
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

import com.github.joschi.jadconfig.Parameter;
import com.github.joschi.jadconfig.validators.InetPortValidator;
import com.github.joschi.jadconfig.validators.PositiveIntegerValidator;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.BusySpinWaitStrategy;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.YieldingWaitStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.InetAddress;
import java.net.URI;

public abstract class BaseConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(BaseConfiguration.class);
    protected static final int GRAYLOG2_DEFAULT_PORT = 12900;

    @Parameter(value = "shutdown_timeout", validator = PositiveIntegerValidator.class)
    protected int shutdownTimeout = 30000;

    @Parameter(value = "rest_transport_uri")
    private URI restTransportUri;

    @Parameter(value = "processbuffer_processors", required = true, validator = PositiveIntegerValidator.class)
    private int processBufferProcessors = 5;

    @Parameter(value = "processor_wait_strategy", required = true)
    private String processorWaitStrategy = "blocking";

    @Parameter(value = "ring_size", required = true, validator = PositiveIntegerValidator.class)
    private int ringSize = 1024;

    @Parameter(value = "rest_enable_cors")
    private boolean restEnableCors = false;

    @Parameter(value = "rest_enable_gzip")
    private boolean restEnableGzip = false;

    @Parameter(value = "rest_max_initial_line_length", required = true, validator = PositiveIntegerValidator.class)
    private int restMaxInitialLineLength = 4096;

    @Parameter(value = "rest_max_header_size", required = true, validator = PositiveIntegerValidator.class)
    private int restMaxHeaderSize = 8192;

    @Parameter(value = "rest_max_chunk_size", required = true, validator = PositiveIntegerValidator.class)
    private int restMaxChunkSize = 8192;

    @Parameter(value = "rest_enable_tls")
    private boolean restEnableTls = false;

    @Parameter(value = "rest_thread_pool_size")
    private int restThreadPoolSize = 16;

    @Parameter(value = "rest_tls_cert_file")
    private File restTlsCertFile;

    @Parameter(value = "rest_tls_key_file")
    private File restTlsKeyFile;

    @Parameter(value = "rest_tls_key_password")
    private String restTlsKeyPassword;

    @Parameter(value = "rest_worker_threads_max_pool_size", required = true, validator = PositiveIntegerValidator.class)
    private int restWorkerThreadsMaxPoolSize = 16;

    @Parameter(value = "groovy_shell_enable")
    private boolean groovyShellEnable = false;

    @Parameter(value = "groovy_shell_port", validator = InetPortValidator.class)
    private int groovyShellPort = 6789;

    @Parameter(value = "plugin_dir")
    private String pluginDir = "plugin";

    @Parameter(value = "async_eventbus_processors")
    private int asyncEventbusProcessors = 2;

    @Parameter(value = "input_cache_max_size")
    private long inputCacheMaxSize = 0;
    @Parameter(value = "udp_recvbuffer_sizes", required = true, validator = PositiveIntegerValidator.class)
    private int udpRecvBufferSizes = 1048576;

    public String getRestUriScheme() {
        return isRestEnableTls() ? "https" : "http";
    }

    public URI getRestTransportUri() {
        return Tools.getUriWithPort(restTransportUri, GRAYLOG2_DEFAULT_PORT);
    }

    public void setRestTransportUri(final URI restTransportUri) {
        this.restTransportUri = restTransportUri;
    }

    public URI getDefaultRestTransportUri() {
        final URI transportUri;
        final URI listenUri = getRestListenUri();

        if ("0.0.0.0".equals(listenUri.getHost())) {
            final InetAddress guessedAddress;
            try {
                guessedAddress = Tools.guessPrimaryNetworkAddress();

                if (guessedAddress.isLoopbackAddress()) {
                    LOG.debug("Using loopback address {}", guessedAddress);
                }
            } catch (Exception e) {
                LOG.error("Could not guess primary network address for rest_transport_uri. Please configure it in your graylog2.conf.", e);
                throw new RuntimeException("No rest_transport_uri.", e);
            }

            transportUri = Tools.getUriWithPort(
                    URI.create("http://" + guessedAddress.getHostAddress() + ":" + listenUri.getPort()), GRAYLOG2_DEFAULT_PORT);
        } else {
            transportUri = listenUri;
        }

        return transportUri;
    }

    public int getProcessBufferProcessors() {
        return processBufferProcessors;
    }

    public WaitStrategy getProcessorWaitStrategy() {
        switch (processorWaitStrategy) {
            case "sleeping":
                return new SleepingWaitStrategy();
            case "yielding":
                return new YieldingWaitStrategy();
            case "blocking":
                return new BlockingWaitStrategy();
            case "busy_spinning":
                return new BusySpinWaitStrategy();
            default:
                LOG.warn("Invalid setting for [processor_wait_strategy]:"
                        + " Falling back to default: BlockingWaitStrategy.");
                return new BlockingWaitStrategy();
        }
    }

    public int getRingSize() {
        return ringSize;
    }

    public boolean isRestEnableCors() {
        return restEnableCors;
    }

    public boolean isRestEnableGzip() {
        return restEnableGzip;
    }

    public int getRestMaxInitialLineLength() {
        return restMaxInitialLineLength;
    }

    public int getRestMaxHeaderSize() {
        return restMaxHeaderSize;
    }

    public int getRestMaxChunkSize() {
        return restMaxChunkSize;
    }

    public boolean isRestEnableTls() {
        return restEnableTls;
    }

    public int getRestThreadPoolSize() {
        return restThreadPoolSize;
    }

    public File getRestTlsCertFile() {
        return restTlsCertFile;
    }

    public File getRestTlsKeyFile() {
        return restTlsKeyFile;
    }

    public String getRestTlsKeyPassword() {
        return restTlsKeyPassword;
    }

    public int getRestWorkerThreadsMaxPoolSize() {
        return restWorkerThreadsMaxPoolSize;
    }

    public boolean isGroovyShellEnable() {
        return groovyShellEnable;
    }

    public int getGroovyShellPort() {
        return groovyShellPort;
    }

    public String getPluginDir() {
        return pluginDir;
    }

    public int getAsyncEventbusProcessors() {
        return asyncEventbusProcessors;
    }

    public long getInputCacheMaxSize() {
        return inputCacheMaxSize;
    }

    public abstract String getNodeIdFile();

    public abstract URI getRestListenUri();

    public int getShutdownTimeout() {
        return shutdownTimeout;
    }

    public int getUdpRecvBufferSizes() {
        return udpRecvBufferSizes;
    }
}

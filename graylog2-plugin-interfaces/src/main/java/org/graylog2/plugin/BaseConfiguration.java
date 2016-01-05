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

import com.github.joschi.jadconfig.Parameter;
import com.github.joschi.jadconfig.util.Duration;
import com.github.joschi.jadconfig.validators.PositiveDurationValidator;
import com.github.joschi.jadconfig.validators.PositiveIntegerValidator;
import com.github.joschi.jadconfig.validators.StringNotBlankValidator;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.BusySpinWaitStrategy;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.YieldingWaitStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.URI;
import java.nio.file.Path;

@SuppressWarnings("FieldMayBeFinal")
public abstract class BaseConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(BaseConfiguration.class);
    protected static final int GRAYLOG_DEFAULT_PORT = 12900;
    protected static final int GRAYLOG_DEFAULT_WEB_PORT = 9000;

    @Parameter(value = "shutdown_timeout", validator = PositiveIntegerValidator.class)
    protected int shutdownTimeout = 30000;

    @Parameter(value = "rest_transport_uri")
    private URI restTransportUri;

    @Parameter(value = "processbuffer_processors", required = true, validator = PositiveIntegerValidator.class)
    private int processBufferProcessors = 5;

    @Parameter(value = "processor_wait_strategy", required = true)
    private String processorWaitStrategy = "blocking";

    @Parameter(value = "ring_size", required = true, validator = PositiveIntegerValidator.class)
    private int ringSize = 65536;

    @Parameter(value = "inputbuffer_ring_size", required = true, validator = PositiveIntegerValidator.class)
    private int inputBufferRingSize = 65536;

    @Parameter(value = "inputbuffer_wait_strategy", required = true)
    private String inputBufferWaitStrategy = "blocking";

    @Parameter(value = "rest_enable_cors")
    private boolean restEnableCors = false;

    @Parameter(value = "rest_enable_gzip")
    private boolean restEnableGzip = false;

    @Parameter(value = "rest_max_initial_line_length", required = true, validator = PositiveIntegerValidator.class)
    private int restMaxInitialLineLength = 4096;

    @Parameter(value = "rest_max_header_size", required = true, validator = PositiveIntegerValidator.class)
    private int restMaxHeaderSize = 8192;

    @Parameter(value = "rest_thread_pool_size", required = true, validator = PositiveIntegerValidator.class)
    private int restThreadPoolSize = 16;

    @Parameter(value = "rest_enable_tls")
    private boolean restEnableTls = false;

    @Parameter(value = "rest_tls_cert_file")
    private Path restTlsCertFile;

    @Parameter(value = "rest_tls_key_file")
    private Path restTlsKeyFile;

    @Parameter(value = "rest_tls_key_password")
    private String restTlsKeyPassword;

    @Parameter(value = "plugin_dir")
    private String pluginDir = "plugin";

    @Parameter(value = "async_eventbus_processors")
    private int asyncEventbusProcessors = 2;

    @Parameter(value = "udp_recvbuffer_sizes", required = true, validator = PositiveIntegerValidator.class)
    private int udpRecvBufferSizes = 1048576;

    @Parameter("message_journal_enabled")
    private boolean messageJournalEnabled = true;

    @Parameter("inputbuffer_processors")
    private int inputbufferProcessors = 2;

    @Parameter("message_recordings_enable")
    private boolean messageRecordingsEnable = false;

    @Parameter("disable_sigar")
    private boolean disableSigar = false;

    @Parameter(value = "http_proxy_uri")
    private URI httpProxyUri;

    @Parameter(value = "http_connect_timeout", validator = PositiveDurationValidator.class)
    private Duration httpConnectTimeout = Duration.seconds(5L);

    @Parameter(value = "http_write_timeout", validator = PositiveDurationValidator.class)
    private Duration httpWriteTimeout = Duration.seconds(10L);

    @Parameter(value = "http_read_timeout", validator = PositiveDurationValidator.class)
    private Duration httpReadTimeout = Duration.seconds(10L);

    @Parameter(value = "installation_source", validator = StringNotBlankValidator.class)
    private String installationSource = "unknown";

    @Parameter(value = "web_enable_cors")
    private boolean webEnableCors = false;

    @Parameter(value = "web_enable_gzip")
    private boolean webEnableGzip = false;

    @Parameter(value = "web_max_initial_line_length", required = true, validator = PositiveIntegerValidator.class)
    private int webMaxInitialLineLength = 4096;

    @Parameter(value = "web_max_header_size", required = true, validator = PositiveIntegerValidator.class)
    private int webMaxHeaderSize = 8192;

    @Parameter(value = "web_enable_tls")
    private boolean webEnableTls = false;

    @Parameter(value = "web_thread_pool_size", required = true, validator = PositiveIntegerValidator.class)
    private int webThreadPoolSize = 16;

    @Parameter(value = "web_tls_cert_file")
    private Path webTlsCertFile;

    @Parameter(value = "web_tls_key_file")
    private Path webTlsKeyFile;

    @Parameter(value = "web_tls_key_password")
    private String webTlsKeyPassword;

    public String getRestUriScheme() {
        return getUriScheme(isRestEnableTls());
    }

    public String getWebUriScheme() {
        return getUriScheme(isWebEnableTls());
    }

    public String getUriScheme(boolean enableTls) {
        return enableTls ? "https" : "http";
    }

    public URI getRestTransportUri() {
        return Tools.getUriWithPort(restTransportUri, GRAYLOG_DEFAULT_PORT);
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
                LOG.error("Could not guess primary network address for \"rest_transport_uri\". Please configure it in your Graylog configuration.", e);
                throw new RuntimeException("No rest_transport_uri.", e);
            }

            transportUri = Tools.getUriWithPort(
                    URI.create("http://" + guessedAddress.getHostAddress() + ":" + listenUri.getPort()), GRAYLOG_DEFAULT_PORT);
        } else {
            transportUri = listenUri;
        }

        return transportUri;
    }

    public int getProcessBufferProcessors() {
        return processBufferProcessors;
    }

    private WaitStrategy getWaitStrategy(String waitStrategyName, String configOptionName) {
        switch (waitStrategyName) {
            case "sleeping":
                return new SleepingWaitStrategy();
            case "yielding":
                return new YieldingWaitStrategy();
            case "blocking":
                return new BlockingWaitStrategy();
            case "busy_spinning":
                return new BusySpinWaitStrategy();
            default:
                LOG.warn("Invalid setting for [{}]:"
                        + " Falling back to default: BlockingWaitStrategy.", configOptionName);
                return new BlockingWaitStrategy();
        }
    }

    public WaitStrategy getProcessorWaitStrategy() {
        return getWaitStrategy(processorWaitStrategy, "processbuffer_wait_strategy");
    }

    public int getRingSize() {
        return ringSize;
    }

    public int getInputBufferRingSize() {
        return inputBufferRingSize;
    }

    public WaitStrategy getInputBufferWaitStrategy() {
        return getWaitStrategy(inputBufferWaitStrategy, "inputbuffer_wait_strategy");
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

    public int getRestThreadPoolSize() {
        return restThreadPoolSize;
    }

    public boolean isRestEnableTls() {
        return restEnableTls;
    }

    public Path getRestTlsCertFile() {
        return restTlsCertFile;
    }

    public Path getRestTlsKeyFile() {
        return restTlsKeyFile;
    }

    public String getRestTlsKeyPassword() {
        return restTlsKeyPassword;
    }

    public String getPluginDir() {
        return pluginDir;
    }

    public int getAsyncEventbusProcessors() {
        return asyncEventbusProcessors;
    }

    public abstract String getNodeIdFile();

    public abstract URI getRestListenUri();

    public abstract URI getWebListenUri();

    public boolean isMessageJournalEnabled() {
        return messageJournalEnabled;
    }

    public void setMessageJournalEnabled(boolean messageJournalEnabled) {
        this.messageJournalEnabled = messageJournalEnabled;
    }

    public int getInputbufferProcessors() {
        return inputbufferProcessors;
    }

    public int getShutdownTimeout() {
        return shutdownTimeout;
    }

    public int getUdpRecvBufferSizes() {
        return udpRecvBufferSizes;
    }

    public boolean isMessageRecordingsEnabled() {
        return messageRecordingsEnable;
    }

    public boolean isDisableSigar() {
        return disableSigar;
    }

    public URI getHttpProxyUri() {
        return httpProxyUri;
    }

    public Duration getHttpConnectTimeout() {
        return httpConnectTimeout;
    }

    public Duration getHttpWriteTimeout() {
        return httpWriteTimeout;
    }

    public Duration getHttpReadTimeout() {
        return httpReadTimeout;
    }

    public String getInstallationSource() {
        return installationSource;
    }

    public boolean isWebEnableCors() {
        return webEnableCors;
    }

    public boolean isWebEnableGzip() {
        return webEnableGzip;
    }

    public int getWebMaxInitialLineLength() {
        return webMaxInitialLineLength;
    }

    public int getWebMaxHeaderSize() {
        return webMaxHeaderSize;
    }

    public boolean isWebEnableTls() {
        return webEnableTls;
    }

    public int getWebThreadPoolSize() {
        return webThreadPoolSize;
    }

    public Path getWebTlsCertFile() {
        return webTlsCertFile;
    }

    public Path getWebTlsKeyFile() {
        return webTlsKeyFile;
    }

    public String getWebTlsKeyPassword() {
        return webTlsKeyPassword;
    }
}

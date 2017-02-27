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

import java.net.URI;

@SuppressWarnings("FieldMayBeFinal")
public abstract class BaseConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(BaseConfiguration.class);

    @Parameter(value = "shutdown_timeout", validator = PositiveIntegerValidator.class)
    protected int shutdownTimeout = 30000;

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

    @Parameter(value = "proxied_requests_thread_pool_size", required = true, validator = PositiveIntegerValidator.class)
    private int proxiedRequestsThreadPoolSize = 32;

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

    public String getPluginDir() {
        return pluginDir;
    }

    public int getAsyncEventbusProcessors() {
        return asyncEventbusProcessors;
    }

    public abstract String getNodeIdFile();

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
}

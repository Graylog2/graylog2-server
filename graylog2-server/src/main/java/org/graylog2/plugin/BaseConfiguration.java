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

import com.github.joschi.jadconfig.Parameter;
import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.ValidatorMethod;
import com.github.joschi.jadconfig.documentation.Documentation;
import com.github.joschi.jadconfig.documentation.DocumentationSection;
import com.github.joschi.jadconfig.util.Duration;
import com.github.joschi.jadconfig.validators.PositiveDurationValidator;
import com.github.joschi.jadconfig.validators.PositiveIntegerValidator;
import com.github.joschi.jadconfig.validators.StringNotBlankValidator;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.BusySpinWaitStrategy;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.YieldingWaitStrategy;
import org.apache.commons.lang3.StringUtils;
import org.graylog2.CommonNodeConfiguration;
import org.graylog2.configuration.PathConfiguration;
import org.graylog2.shared.messageq.MessageQueueModule;
import org.graylog2.utilities.ProxyHostsPattern;
import org.graylog2.utilities.ProxyHostsPatternConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

import static org.graylog2.shared.messageq.MessageQueueModule.DISK_JOURNAL_MODE;
import static org.graylog2.shared.messageq.MessageQueueModule.NOOP_JOURNAL_MODE;

@SuppressWarnings("FieldMayBeFinal")
@DocumentationSection(heading = "System Parameters", description = "")
public abstract class BaseConfiguration extends PathConfiguration implements CommonNodeConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(BaseConfiguration.class);

    @Documentation("Time in milliseconds which Graylog is waiting for all threads to stop on shutdown.")
    @Parameter(value = "shutdown_timeout", validator = PositiveIntegerValidator.class)
    protected int shutdownTimeout = 30000;

    @Documentation("""
            Number of process buffer processors running in parallel.
            By default, the value will be determined automatically based on the number of CPU cores available to the JVM, using
            the formula (<#cores> * 0.36 + 0.625) rounded to the nearest integer.
            Set this value explicitly to override the dynamically calculated value. Try raising the number if your buffers are
            filling up.
            """)
    @Parameter(value = "processbuffer_processors", required = true, validator = PositiveIntegerValidator.class)
    private int processBufferProcessors = defaultNumberOfProcessBufferProcessors();

    @Documentation("""
            Wait strategy describing how buffer processors wait on a cursor sequence. (default: sleeping)
            Possible types:
              - yielding
                Compromise between performance and CPU usage.
              - sleeping
                Compromise between performance and CPU usage. Latency spikes can occur after quiet periods.
              - blocking
                High throughput, low latency, higher CPU usage.
              - busy_spinning
                Avoids syscalls which could introduce latency jitter. Best when threads can be bound to specific CPU cores.
            """)
    @Parameter(value = "processor_wait_strategy", required = true)
    private String processorWaitStrategy = "blocking";

    @Documentation("""
            Size of internal ring buffers. Raise this if raising outputbuffer_processors does not help anymore.
            For optimum performance your LogMessage objects in the ring buffer should fit in your CPU L3 cache.
            Must be a power of 2. (512, 1024, 2048, ...)
            """)
    @Parameter(value = "ring_size", required = true, validator = PositiveIntegerValidator.class)
    private int ringSize = 65536;

    @Documentation("tbd")
    @Parameter(value = "inputbuffer_ring_size", required = true, validator = PositiveIntegerValidator.class)
    private int inputBufferRingSize = 65536;

    @Documentation("tbd")
    @Parameter(value = "inputbuffer_wait_strategy", required = true)
    private String inputBufferWaitStrategy = "blocking";

    @Documentation("Number of threads used exclusively for dispatching internal events. Default is 2.")
    @Parameter(value = "async_eventbus_processors")
    private int asyncEventbusProcessors = 2;

    @Documentation("UDP receive buffer size for all message inputs (e. g. SyslogUDPInput).")
    @Parameter(value = "udp_recvbuffer_sizes", required = true, validator = PositiveIntegerValidator.class)
    private int udpRecvBufferSizes = 1048576;

    @Documentation("Enable the message journal.")
    @Parameter("message_journal_enabled")
    private boolean messageJournalEnabled = true;

    @Documentation("tbd")
    @Parameter(value = "message_journal_mode")
    private String messageJournalMode = MessageQueueModule.DISK_JOURNAL_MODE;

    @Documentation("Number of input buffer processors running in parallel.")
    @Parameter("inputbuffer_processors")
    private int inputbufferProcessors = 2;

    @Documentation("tbd")
    @Parameter("message_recordings_enable")
    private boolean messageRecordingsEnable = false;

    @Documentation("Disable the use of a native system stats collector (currently OSHI)")
    @Parameter("disable_native_system_stats_collector")
    private boolean disableNativeSystemStatsCollector = false;

    @Documentation("""
            The User-Agent header for outgoing HTTP connections.
            Default: Graylog
            """)
    @Parameter(value = "http_user_agent")
    private String httpUserAgent = "Graylog";

    @Documentation("""
            HTTP proxy for outgoing HTTP connections
            ATTENTION: If you configure a proxy, make sure to also configure the "http_non_proxy_hosts" option so internal
                       HTTP connections with other nodes does not go through the proxy.
            Examples:
              - http://proxy.example.com:8123
              - http://username:password@proxy.example.com:8123
            """)
    @Parameter(value = "http_proxy_uri")
    private URI httpProxyUri;

    @Documentation("""
            A list of hosts that should be reached directly, bypassing the configured proxy server.
            This is a list of patterns separated by ",". The patterns may start or end with a "*" for wildcards.
            Any host matching one of these patterns will be reached through a direct connection instead of through a proxy.
            Examples:
              - localhost,127.0.0.1
              - 10.0.*,*.example.com
            """)
    @Parameter(value = "http_non_proxy_hosts", converter = ProxyHostsPatternConverter.class)
    private ProxyHostsPattern httpNonProxyHostsPattern;

    @Documentation("""
            The default connect timeout for outgoing HTTP connections.
            Values must be a positive duration (and between 1 and 2147483647 when converted to milliseconds).
            Default: 5s
            """)
    @Parameter(value = "http_connect_timeout", validator = PositiveDurationValidator.class)
    private Duration httpConnectTimeout = Duration.seconds(5L);

    @Documentation("""
            The default write timeout for outgoing HTTP connections.
            Values must be a positive duration (and between 1 and 2147483647 when converted to milliseconds).
            Default: 10s
            """)
    @Parameter(value = "http_write_timeout", validator = PositiveDurationValidator.class)
    private Duration httpWriteTimeout = Duration.seconds(10L);

    @Documentation("""
            The default read timeout for outgoing HTTP connections.
            Values must be a positive duration (and between 1 and 2147483647 when converted to milliseconds).
            Default: 10s
            """)
    @Parameter(value = "http_read_timeout", validator = PositiveDurationValidator.class)
    private Duration httpReadTimeout = Duration.seconds(10L);

    @Documentation("tbd")
    @Parameter(value = "installation_source", validator = StringNotBlankValidator.class)
    private String installationSource = "unknown";

    @Documentation("""
            For some cluster-related REST requests, the node must query all other nodes in the cluster. This is the maximum number
            of threads available for this. Increase it, if '/cluster/*' requests take long to complete.
            Should be http_thread_pool_size * average_cluster_size if you have a high number of concurrent users.
            """)
    @Parameter(value = "proxied_requests_thread_pool_size", required = true, validator = PositiveIntegerValidator.class)
    private int proxiedRequestsThreadPoolSize = 64;

    @Documentation("""
            The default HTTP call timeout for cluster-related REST requests. This timeout might be overriden for some
            resources in code or other configuration values. (some cluster metrics resources use a lower timeout)
            """)
    @Parameter(value = "proxied_requests_default_call_timeout", required = true, validator = PositiveDurationValidator.class)
    private Duration proxiedRequestsDefaultCallTimeout = Duration.seconds(5);

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

    public int getAsyncEventbusProcessors() {
        return asyncEventbusProcessors;
    }

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

    @Override
    public boolean isMessageRecordingsEnabled() {
        return messageRecordingsEnable;
    }

    public boolean isDisableNativeSystemStatsCollector() {
        return disableNativeSystemStatsCollector;
    }

    public String getHttpUserAgent() {
        return httpUserAgent;
    }

    public URI getHttpProxyUri() {
        return httpProxyUri;
    }

    public ProxyHostsPattern getHttpNonProxyHostsPattern() {
        return httpNonProxyHostsPattern;
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

    /**
     * Journal mode will be "noop" if the journal is disabled or the configured journal mode otherwise.
     */
    public String getMessageJournalMode() {
        return messageJournalEnabled ? messageJournalMode : NOOP_JOURNAL_MODE;
    }

    @ValidatorMethod
    public void validateJournalMode() throws ValidationException {
        if (!messageJournalEnabled) {
            return;
        }

        // the noop implementation is not fully functional and relies on the journal mode being disabled because
        // otherwise message would be lost.
        if (messageJournalMode.equals(NOOP_JOURNAL_MODE)) {
            throw new ValidationException("Setting message journal mode to <" + NOOP_JOURNAL_MODE +
                    "> without disabling the message journal is not supported.");
        }

        if (StringUtils.isBlank(messageJournalMode)) {
            throw new ValidationException("Journal mode (e.g. <" + DISK_JOURNAL_MODE + ">) needs to be " +
                    "provided when the journal is enabled.");
        }
    }

    /**
     * Calculate the default number of process buffer processors as a linear function of available CPU cores.
     * The function is designed to yield predetermined values for the following select numbers of CPU cores that
     * have proven to work well in real-world production settings:
     * <table>
     *     <tr>
     *         <th># CPU cores</th><th># buffer processors</th>
     *     </tr>
     *     <tr>
     *         <td>2</td><td>1</td>
     *     </tr>
     *     <tr>
     *         <td>4</td><td>2</td>
     *     </tr>
     *     <tr>
     *         <td>8</td><td>4</td>
     *     </tr>
     *     <tr>
     *         <td>12</td><td>5</td>
     *     </tr>
     *     <tr>
     *         <td>16</td><td>6</td>
     *     </tr>
     * </table>
     */
    private static int defaultNumberOfProcessBufferProcessors() {
        return Math.round(Tools.availableProcessors() * 0.36f + 0.625f);
    }
}

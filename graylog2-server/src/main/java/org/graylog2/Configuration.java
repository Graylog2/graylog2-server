/**
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
package org.graylog2;

import com.github.joschi.jadconfig.Parameter;
import com.github.joschi.jadconfig.util.Duration;
import com.github.joschi.jadconfig.validators.PositiveIntegerValidator;
import com.github.joschi.jadconfig.validators.PositiveLongValidator;
import org.graylog2.plugin.BaseConfiguration;
import org.joda.time.DateTimeZone;

import java.net.URI;

import static org.graylog2.plugin.Tools.getUriWithDefaultPath;
import static org.graylog2.plugin.Tools.getUriWithPort;
import static org.graylog2.plugin.Tools.getUriWithScheme;

/**
 * Helper class to hold configuration of Graylog2
 */
@SuppressWarnings("FieldMayBeFinal")
public class Configuration extends BaseConfiguration {
    @Parameter(value = "is_master", required = true)
    private boolean isMaster = true;

    @Parameter(value = "password_secret", required = true)
    private String passwordSecret;

    @Parameter(value = "rest_listen_uri", required = true)
    private URI restListenUri = URI.create("http://127.0.0.1:" + GRAYLOG2_DEFAULT_PORT + "/");

    @Parameter(value = "output_batch_size", required = true, validator = PositiveIntegerValidator.class)
    private int outputBatchSize = 500;

    @Parameter(value = "output_flush_interval", required = true, validator = PositiveIntegerValidator.class)
    private int outputFlushInterval = 1;

    @Parameter(value = "outputbuffer_processors", required = true, validator = PositiveIntegerValidator.class)
    private int outputBufferProcessors = 3;

    @Parameter(value = "outputbuffer_processor_threads_max_pool_size", required = true, validator = PositiveIntegerValidator.class)
    private int outputBufferProcessorThreadsMaxPoolSize = 30;

    @Parameter(value = "outputbuffer_processor_threads_core_pool_size", required = true, validator = PositiveIntegerValidator.class)
    private int outputBufferProcessorThreadsCorePoolSize = 3;

    @Parameter(value = "outputbuffer_processor_keep_alive_time", validator = PositiveIntegerValidator.class)
    private int outputBufferProcessorKeepAliveTime = 5000;

    @Parameter(value = "dead_letters_enabled")
    private boolean deadLettersEnabled = false;

    @Parameter("rules_file")
    private String droolsRulesFile;

    @Parameter(value = "node_id_file")
    private String nodeIdFile = "/etc/graylog2-server-node-id";

    @Parameter(value = "root_username")
    private String rootUsername = "admin";

    @Parameter(value = "root_password_sha2", required = true)
    private String rootPasswordSha2;

    @Parameter(value = "root_timezone")
    private DateTimeZone rootTimeZone = DateTimeZone.UTC;

    @Parameter(value = "root_email")
    private String rootEmail = "";

    @Parameter(value = "allow_leading_wildcard_searches")
    private boolean allowLeadingWildcardSearches = false;

    @Parameter(value = "allow_highlighting")
    private boolean allowHighlighting = false;

    @Parameter(value = "enable_metrics_collection")
    private boolean metricsCollectionEnabled = false;

    @Parameter(value = "lb_recognition_period_seconds", validator = PositiveIntegerValidator.class)
    private int loadBalancerRecognitionPeriodSeconds = 3;

    @Parameter(value = "stream_processing_timeout", validator = PositiveLongValidator.class)
    private long streamProcessingTimeout = 2000;

    @Parameter(value = "stream_processing_max_faults", validator = PositiveIntegerValidator.class)
    private int streamProcessingMaxFaults = 3;

    @Parameter(value = "output_module_timeout", validator = PositiveLongValidator.class)
    private long outputModuleTimeout = 10000;

    @Parameter(value = "output_fault_count_threshold", validator = PositiveLongValidator.class)
    private long outputFaultCountThreshold = 5;

    @Parameter(value = "output_fault_penalty_seconds", validator = PositiveLongValidator.class)
    private long outputFaultPenaltySeconds = 30;

    @Parameter(value = "stale_master_timeout", validator = PositiveIntegerValidator.class)
    private int staleMasterTimeout = 2000;

    @Parameter(value = "ldap_connection_timeout", validator = PositiveIntegerValidator.class)
    private int ldapConnectionTimeout = 2000;

    @Parameter(value = "alert_check_interval", validator = PositiveIntegerValidator.class)
    private int alertCheckInterval = 60;

    @Parameter(value = "gc_warning_threshold")
    private Duration gcWarningThreshold = Duration.seconds(1l);

    @Parameter(value = "default_message_output_class")
    private String defaultMessageOutputClass = "";

    public boolean isMaster() {
        return isMaster;
    }

    public void setIsMaster(boolean is) {
        isMaster = is;
    }

    public String getPasswordSecret() {
        return passwordSecret.trim();
    }

    public int getOutputBatchSize() {
        return outputBatchSize;
    }

    public int getOutputFlushInterval() {
        return outputFlushInterval;
    }

    public int getOutputBufferProcessors() {
        return outputBufferProcessors;
    }

    public int getOutputBufferProcessorThreadsCorePoolSize() {
        return outputBufferProcessorThreadsCorePoolSize;
    }

    public int getOutputBufferProcessorThreadsMaxPoolSize() {
        return outputBufferProcessorThreadsMaxPoolSize;
    }

    public int getOutputBufferProcessorKeepAliveTime() {
        return outputBufferProcessorKeepAliveTime;
    }

    public String getDroolsRulesFile() {
        return droolsRulesFile;
    }

    public String getNodeIdFile() {
        return nodeIdFile;
    }

    @Override
    public URI getRestListenUri() {
        return getUriWithDefaultPath(getUriWithPort(getUriWithScheme(restListenUri, getRestUriScheme()), GRAYLOG2_DEFAULT_PORT), "/");
    }

    public String getRootUsername() {
        return rootUsername;
    }

    public String getRootPasswordSha2() {
        return rootPasswordSha2;
    }

    public DateTimeZone getRootTimeZone() {
        return rootTimeZone;
    }

    public String getRootEmail() {
        return rootEmail;
    }

    public boolean isAllowLeadingWildcardSearches() {
        return allowLeadingWildcardSearches;
    }

    public boolean isAllowHighlighting() {
        return allowHighlighting;
    }

    public boolean isMetricsCollectionEnabled() {
        return metricsCollectionEnabled;
    }

    public boolean isDeadLettersEnabled() {
        return deadLettersEnabled;
    }

    public int getLoadBalancerRecognitionPeriodSeconds() {
        return loadBalancerRecognitionPeriodSeconds;
    }

    public long getStreamProcessingTimeout() {
        return streamProcessingTimeout;
    }

    public int getStreamProcessingMaxFaults() {
        return streamProcessingMaxFaults;
    }

    public long getOutputModuleTimeout() {
        return outputModuleTimeout;
    }

    public long getOutputFaultCountThreshold() {
        return outputFaultCountThreshold;
    }

    public long getOutputFaultPenaltySeconds() {
        return outputFaultPenaltySeconds;
    }

    public int getStaleMasterTimeout() {
        return staleMasterTimeout;
    }

    public int getLdapConnectionTimeout() {
        return ldapConnectionTimeout;
    }

    public int getAlertCheckInterval() {
        return alertCheckInterval;
    }

    public Duration getGcWarningThreshold() {
        return gcWarningThreshold;
    }

    public String getDefaultMessageOutputClass() {
        return defaultMessageOutputClass;
    }
}

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
package org.graylog2;

import com.github.joschi.jadconfig.Parameter;
import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.ValidatorMethod;
import com.github.joschi.jadconfig.converters.TrimmedStringSetConverter;
import com.github.joschi.jadconfig.util.Duration;
import com.github.joschi.jadconfig.validators.DirectoryPathReadableValidator;
import com.github.joschi.jadconfig.validators.PositiveDurationValidator;
import com.github.joschi.jadconfig.validators.PositiveIntegerValidator;
import com.github.joschi.jadconfig.validators.PositiveLongValidator;
import com.github.joschi.jadconfig.validators.StringNotBlankValidator;
import com.github.joschi.jadconfig.validators.URIAbsoluteValidator;
import org.graylog2.plugin.BaseConfiguration;
import org.graylog2.utilities.IPSubnetConverter;
import org.jboss.netty.handler.ipfilter.IpSubnet;
import org.joda.time.DateTimeZone;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Set;

import static org.graylog2.plugin.Tools.normalizeURI;

/**
 * Helper class to hold configuration of Graylog
 */
@SuppressWarnings("FieldMayBeFinal")
public class Configuration extends BaseConfiguration {
    @Parameter(value = "is_master", required = true)
    private boolean isMaster = true;

    @Parameter(value = "password_secret", required = true, validator = StringNotBlankValidator.class)
    private String passwordSecret;

    @Parameter(value = "rest_listen_uri", required = true, validator = URIAbsoluteValidator.class)
    private URI restListenUri = URI.create("http://127.0.0.1:" + GRAYLOG_DEFAULT_PORT + "/api/");

    @Parameter(value = "web_listen_uri", required = true, validator = URIAbsoluteValidator.class)
    private URI webListenUri = URI.create("http://127.0.0.1:" + GRAYLOG_DEFAULT_WEB_PORT + "/");

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

    @Parameter("rules_file")
    private String droolsRulesFile;

    @Parameter(value = "node_id_file")
    private String nodeIdFile = "/etc/graylog/server/node-id";

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

    @Parameter(value = "lb_recognition_period_seconds", validator = PositiveIntegerValidator.class)
    private int loadBalancerRecognitionPeriodSeconds = 3;

    @Parameter(value = "lb_throttle_threshold_percentage", validator = PositiveIntegerValidator.class)
    private int loadBalancerThrottleThresholdPercentage = 100;

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
    private Duration gcWarningThreshold = Duration.seconds(1L);

    @Parameter(value = "default_message_output_class")
    private String defaultMessageOutputClass = "";

    @Parameter(value = "dashboard_widget_default_cache_time", validator = PositiveDurationValidator.class)
    private Duration dashboardWidgetDefaultCacheTime = Duration.seconds(10L);

    @Parameter(value = "user_password_default_algorithm")
    private String userPasswordDefaultAlgorithm = "bcrypt";

    @Parameter(value = "user_password_bcrypt_salt_size", validator = PositiveIntegerValidator.class)
    private int userPasswordBCryptSaltSize = 10;

    @Parameter(value = "content_packs_loader_enabled")
    private boolean contentPacksLoaderEnabled = true;

    @Parameter(value = "content_packs_dir", validators = DirectoryPathReadableValidator.class)
    private Path contentPacksDir = Paths.get("data", "contentpacks");

    @Parameter(value = "content_packs_auto_load", converter = TrimmedStringSetConverter.class)
    private Set<String> contentPacksAutoLoad = Collections.emptySet();

    @Parameter(value = "index_ranges_cleanup_interval", validator = PositiveDurationValidator.class)
    private Duration indexRangesCleanupInterval = Duration.hours(1L);

    @Parameter(value = "trusted_proxies", converter = IPSubnetConverter.class)
    private Set<IpSubnet> trustedProxies = Collections.emptySet();

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

    @Override
    public String getNodeIdFile() {
        return nodeIdFile;
    }

    @Override
    public URI getRestListenUri() {
        return normalizeURI(restListenUri, getRestUriScheme(), GRAYLOG_DEFAULT_PORT, "/");
    }

    @Override
    public URI getWebListenUri() {
        return normalizeURI(webListenUri, getWebUriScheme(), GRAYLOG_DEFAULT_WEB_PORT, "/");
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

    public Duration getDashboardWidgetDefaultCacheTime() {
        return dashboardWidgetDefaultCacheTime;
    }

    public String getUserPasswordDefaultAlgorithm() {
        return userPasswordDefaultAlgorithm;
    }

    public int getUserPasswordBCryptSaltSize() {
        return userPasswordBCryptSaltSize;
    }

    public boolean isContentPacksLoaderEnabled() {
        return contentPacksLoaderEnabled;
    }

    public Path getContentPacksDir() {
        return contentPacksDir;
    }

    public Set<String> getContentPacksAutoLoad() {
        return contentPacksAutoLoad;
    }

    public Duration getIndexRangesCleanupInterval() {
        return indexRangesCleanupInterval;
    }

    public Set<IpSubnet> getTrustedProxies() {
        return trustedProxies;
    }

    public int getLoadBalancerRequestThrottleJournalUsage() {
        return loadBalancerThrottleThresholdPercentage;
    }

    @ValidatorMethod
    @SuppressWarnings("unused")
    public void validatePasswordSecret() throws ValidationException {
        final String passwordSecret = getPasswordSecret();
        if (passwordSecret == null || passwordSecret.length() < 16) {
            throw new ValidationException("The minimum length for \"password_secret\" is 16 characters.");
        }
    }

    @ValidatorMethod
    @SuppressWarnings("unused")
    public void validateNetworkInterfaces() throws ValidationException {
        final URI restListenUri = getRestListenUri();
        final URI webListenUri = getWebListenUri();

        if (restListenUri.getPort() == webListenUri.getPort() &&
                !restListenUri.getHost().equals(webListenUri.getHost()) &&
                (WILDCARD_IP_ADDRESS.equals(restListenUri.getHost()) || WILDCARD_IP_ADDRESS.equals(webListenUri.getHost()))) {
            throw new ValidationException("Wildcard IP addresses cannot be used if the Graylog REST API and web interface listen on the same port.");
        }
    }
}

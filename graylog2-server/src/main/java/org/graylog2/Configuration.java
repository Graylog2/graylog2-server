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
package org.graylog2;

import com.github.joschi.jadconfig.Parameter;
import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.Validator;
import com.github.joschi.jadconfig.ValidatorMethod;
import com.github.joschi.jadconfig.converters.StringSetConverter;
import com.github.joschi.jadconfig.converters.TrimmedStringSetConverter;
import com.github.joschi.jadconfig.util.Duration;
import com.github.joschi.jadconfig.validators.PositiveDurationValidator;
import com.github.joschi.jadconfig.validators.PositiveIntegerValidator;
import com.github.joschi.jadconfig.validators.PositiveLongValidator;
import com.github.joschi.jadconfig.validators.StringNotBlankValidator;
import org.graylog2.plugin.BaseConfiguration;
import org.graylog2.security.realm.RootAccountRealm;
import org.graylog2.shared.security.tls.DefaultTLSProtocolProvider;
import org.graylog2.utilities.IPSubnetConverter;
import org.graylog2.utilities.IpSubnet;
import org.joda.time.DateTimeZone;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Set;

/**
 * Helper class to hold configuration of Graylog
 */
@SuppressWarnings("FieldMayBeFinal")
public class Configuration extends BaseConfiguration {
    @Parameter(value = "is_master", required = true)
    private boolean isMaster = true;

    @Parameter(value = "password_secret", required = true, validator = StringNotBlankValidator.class)
    private String passwordSecret;

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

    @Parameter(value = "node_id_file", validator = NodeIdFileValidator.class)
    private String nodeIdFile = "/etc/graylog/server/node-id";

    @Parameter(value = "root_username")
    private String rootUsername = "admin";

    // Required unless "root-user" is deactivated in the "deactivated_builtin_authentication_providers" setting
    @Parameter(value = "root_password_sha2")
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
    @Deprecated
    private int alertCheckInterval = 60;

    @Parameter(value = "enable_legacy_alerts")
    @Deprecated
    private boolean enableLegacyAlerts = false;

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
    private boolean contentPacksLoaderEnabled = false;

    @Parameter(value = "content_packs_dir")
    private Path contentPacksDir = DEFAULT_DATA_DIR.resolve("contentpacks");

    @Parameter(value = "content_packs_auto_install", converter = TrimmedStringSetConverter.class)
    private Set<String> contentPacksAutoInstall = Collections.emptySet();

    @Parameter(value = "index_ranges_cleanup_interval", validator = PositiveDurationValidator.class)
    private Duration indexRangesCleanupInterval = Duration.hours(1L);

    @Parameter(value = "trusted_proxies", converter = IPSubnetConverter.class)
    private Set<IpSubnet> trustedProxies = Collections.emptySet();

    @Parameter(value = "deactivated_builtin_authentication_providers", converter = StringSetConverter.class)
    private Set<String> deactivatedBuiltinAuthenticationProviders = Collections.emptySet();

    @Parameter(value = "enabled_tls_protocols", converter = StringSetConverter.class)
    private Set<String> enabledTlsProtocols = DefaultTLSProtocolProvider.getDefaultSupportedTlsProtocols();

    @Parameter(value = "elasticsearch_mute_deprecation_warnings")
    private boolean muteDeprecationWarnings = false;

    public boolean isMuteDeprecationWarnings() {
        return muteDeprecationWarnings;
    }

    public void setMuteDeprecationWarnings(boolean muteDeprecationWarnings) {
        this.muteDeprecationWarnings = muteDeprecationWarnings;
    }

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

    @Override
    public String getNodeIdFile() {
        return nodeIdFile;
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

    @Deprecated
    public int getAlertCheckInterval() {
        return alertCheckInterval;
    }

    @Deprecated
    public boolean isEnableLegacyAlerts() {
        return enableLegacyAlerts;
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

    public Set<String> getContentPacksAutoInstall() {
        return contentPacksAutoInstall;
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

    public Set<String> getDeactivatedBuiltinAuthenticationProviders() {
        return deactivatedBuiltinAuthenticationProviders;
    }

    public Set<String> getEnabledTlsProtocols() {
        return enabledTlsProtocols;
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
    public void validateRootUser() throws ValidationException {
        if (getRootPasswordSha2() == null && !isRootUserDisabled()) {
            throw new ValidationException("Required parameter \"root_password_sha2\" not found.");
        }
    }

    /**
     * The root user is disabled if the {@link RootAccountRealm} is deactivated.
     */
    public boolean isRootUserDisabled() {
        return getDeactivatedBuiltinAuthenticationProviders().contains(RootAccountRealm.NAME);
    }

    public static class NodeIdFileValidator implements Validator<String> {
        @Override
        public void validate(String name, String path) throws ValidationException {
            if (path == null) {
                return;
            }
            final File file = Paths.get(path).toFile();
            final StringBuilder b = new StringBuilder();

            if (!file.exists()) {
                final File parent = file.getParentFile();
                if (!parent.isDirectory()) {
                    throw new ValidationException("Parent path " + parent + " for Node ID file at " + path + " is not a directory");
                } else {
                    if (!parent.canRead()) {
                        throw new ValidationException("Parent directory " + parent + " for Node ID file at " + path + " is not readable");
                    }
                    if (!parent.canWrite()) {
                        throw new ValidationException("Parent directory " + parent + " for Node ID file at " + path + " is not writable");
                    }

                    // parent directory exists and is readable and writable
                    return;
                }
            }

            if (!file.isFile()) {
                b.append("a file");
            }
            final boolean readable = file.canRead();
            final boolean writable = file.canWrite();
            if (!readable) {
                if (b.length() > 0) {
                    b.append(", ");
                }
                b.append("readable");
            }
            final boolean empty = file.length() == 0;
            if (!writable && readable && empty) {
                if (b.length() > 0) {
                    b.append(", ");
                }
                b.append("writable, but it is empty");
            }
            if (b.length() == 0) {
                // all good
                return;
            }
            throw new ValidationException("Node ID file at path " + path + " isn't " + b +". Please specify the correct path or change the permissions");
        }
    }
}

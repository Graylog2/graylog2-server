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
import org.graylog2.cluster.leader.AutomaticLeaderElectionService;
import org.graylog2.cluster.leader.LeaderElectionMode;
import org.graylog2.cluster.leader.LeaderElectionService;
import org.graylog2.cluster.lock.MongoLockService;
import org.graylog2.configuration.converters.JavaDurationConverter;
import org.graylog2.plugin.BaseConfiguration;
import org.graylog2.security.realm.RootAccountRealm;
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

    /**
     * Deprecated! Use isLeader() instead.
     */
    @Parameter(value = "is_master")
    private boolean isMaster = true;

    @Parameter(value = "stream_aware_field_types")
    private boolean streamAwareFieldTypes = false;

    /**
     * Used for initializing static leader election. You shouldn't use this for other purposes, but if you must, don't
     * use @{@link javax.inject.Named} injection but the getter isLeader() instead.
     **/
    @Parameter(value = "is_leader")
    private Boolean isLeader;

    @Parameter(value = "password_secret", required = true, validators = StringNotBlankValidator.class)
    private String passwordSecret;

    @Parameter(value = "output_batch_size", required = true, validators = PositiveIntegerValidator.class)
    private int outputBatchSize = 500;

    @Parameter(value = "output_flush_interval", required = true, validators = PositiveIntegerValidator.class)
    private int outputFlushInterval = 1;

    @Parameter(value = "outputbuffer_processors", required = true, validators = PositiveIntegerValidator.class)
    private int outputBufferProcessors = 3;

    @Parameter(value = "outputbuffer_processor_threads_max_pool_size", required = true, validators = PositiveIntegerValidator.class)
    private int outputBufferProcessorThreadsMaxPoolSize = 30;

    @Parameter(value = "outputbuffer_processor_threads_core_pool_size", required = true, validators = PositiveIntegerValidator.class)
    private int outputBufferProcessorThreadsCorePoolSize = 3;

    @Parameter(value = "outputbuffer_processor_keep_alive_time", validators = PositiveIntegerValidator.class)
    private int outputBufferProcessorKeepAliveTime = 5000;

    @Parameter(value = "node_id_file", validators = NodeIdFileValidator.class)
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

    @Parameter(value = "lb_recognition_period_seconds", validators = PositiveIntegerValidator.class)
    private int loadBalancerRecognitionPeriodSeconds = 3;

    @Parameter(value = "lb_throttle_threshold_percentage", validators = PositiveIntegerValidator.class)
    private int loadBalancerThrottleThresholdPercentage = 100;

    @Parameter(value = "stream_processing_timeout", validators = PositiveLongValidator.class)
    private long streamProcessingTimeout = 2000;

    @Parameter(value = "stream_processing_max_faults", validators = PositiveIntegerValidator.class)
    private int streamProcessingMaxFaults = 3;

    @Parameter(value = "output_module_timeout", validators = PositiveLongValidator.class)
    private long outputModuleTimeout = 10000;

    @Parameter(value = "output_fault_count_threshold", validators = PositiveLongValidator.class)
    private long outputFaultCountThreshold = 5;

    @Parameter(value = "output_fault_penalty_seconds", validators = PositiveLongValidator.class)
    private long outputFaultPenaltySeconds = 30;

    /**
     * Deprecated! Use staleLeaderTimeout instead
     */
    @Parameter(value = "stale_master_timeout", validators = PositiveIntegerValidator.class)
    private int staleMasterTimeout = 2000;

    /**
     * Don't use @{@link javax.inject.Named} injection but the getter getStaleLeaderTimeout() instead.
     **/
    @Parameter(value = "stale_leader_timeout", validators = PositiveIntegerValidator.class)
    private Integer staleLeaderTimeout;

    @Parameter(value = "ldap_connection_timeout", validators = PositiveIntegerValidator.class)
    private int ldapConnectionTimeout = 2000;

    @Parameter(value = "alert_check_interval", validators = PositiveIntegerValidator.class)
    @Deprecated
    private int alertCheckInterval = 60;

    @Parameter(value = "gc_warning_threshold")
    private Duration gcWarningThreshold = Duration.seconds(1L);

    @Parameter(value = "default_message_output_class")
    private String defaultMessageOutputClass = "";

    @Parameter(value = "dashboard_widget_default_cache_time", validators = PositiveDurationValidator.class)
    private Duration dashboardWidgetDefaultCacheTime = Duration.seconds(10L);

    @Parameter(value = "user_password_default_algorithm")
    private String userPasswordDefaultAlgorithm = "bcrypt";

    @Parameter(value = "user_password_bcrypt_salt_size", validators = PositiveIntegerValidator.class)
    private int userPasswordBCryptSaltSize = 10;

    @Parameter(value = "content_packs_loader_enabled")
    private boolean contentPacksLoaderEnabled = false;

    @Parameter(value = "content_packs_dir")
    private Path contentPacksDir = DEFAULT_DATA_DIR.resolve("contentpacks");

    @Parameter(value = "content_packs_auto_install", converter = TrimmedStringSetConverter.class)
    private Set<String> contentPacksAutoInstall = Collections.emptySet();

    @Parameter(value = "index_ranges_cleanup_interval", validators = PositiveDurationValidator.class)
    private Duration indexRangesCleanupInterval = Duration.hours(1L);

    @Parameter(value = "trusted_proxies", converter = IPSubnetConverter.class)
    private Set<IpSubnet> trustedProxies = Collections.emptySet();

    @Parameter(value = "deactivated_builtin_authentication_providers", converter = StringSetConverter.class)
    private Set<String> deactivatedBuiltinAuthenticationProviders = Collections.emptySet();

    // This is needed for backwards compatibility. The setting in TLSProtocolsConfiguration should be used instead.
    @Parameter(value = "enabled_tls_protocols", converter = StringSetConverter.class)
    private Set<String> enabledTlsProtocols = null;

    @Parameter(value = "failure_handling_queue_capacity", validators = {PositiveIntegerValidator.class})
    private int failureHandlingQueueCapacity = 1000;

    @Parameter(value = "failure_handling_shutdown_await", validators = {PositiveDurationValidator.class})
    private Duration failureHandlingShutdownAwait = Duration.milliseconds(3000);

    @Parameter(value = "is_cloud")
    private boolean isCloud = false;

    @Parameter(value = "auto_restart_inputs")
    private boolean autoRestartInputs = false;

    @Parameter(value = "run_migrations")
    private boolean runMigrations = true;

    @Parameter(value = "ignore_migration_failures")
    private boolean ignoreMigrationFailures = false;

    @Parameter(value = "skip_preflight_checks")
    private boolean skipPreflightChecks = false;

    @Parameter(value = "leader_election_mode", converter = LeaderElectionMode.Converter.class)
    private LeaderElectionMode leaderElectionMode = LeaderElectionMode.STATIC;

    @Parameter(value = "leader_election_lock_polling_interval", converter = JavaDurationConverter.class)
    private java.time.Duration leaderElectionLockPollingInterval = AutomaticLeaderElectionService.DEFAULT_POLLING_INTERVAL;

    @Parameter(value = "lock_service_lock_ttl", converter = JavaDurationConverter.class)
    private java.time.Duration lockServiceLockTTL = MongoLockService.MIN_LOCK_TTL;

    public boolean maintainsStreamAwareFieldTypes() {
        return streamAwareFieldTypes;
    }

    /**
     * @deprecated Use {@link #isLeader()} instead.
     */
    @Deprecated
    public boolean isMaster() {
        return isLeader();
    }

    /**
     * Returns the <em>configured</em> leader status. This is only valid for static leader election. You should probably
     * use {@link LeaderElectionService#isLeader()} instead.
     */
    public boolean isLeader() {
        return isLeader != null ? isLeader : isMaster;
    }

    /**
     * @deprecated Use {@link #setIsLeader(boolean)} instead
     */
    @Deprecated
    public void setIsMaster(boolean is) {
        setIsLeader(is);
    }

    /**
     * We should remove this method after refactoring {@link org.graylog2.cluster.leader.StaticLeaderElectionService}
     * and {@link org.graylog2.commands.Server} so that they don't need this to communicate demotion from leader to
     * follower anymore.
     */
    public void setIsLeader(boolean is) {
        isLeader = isMaster = is;
    }

    public LeaderElectionMode getLeaderElectionMode() {
        return leaderElectionMode;
    }

    public java.time.Duration getLockServiceLockTTL() {
        return lockServiceLockTTL;
    }

    public java.time.Duration getLeaderElectionLockPollingInterval() {
        return leaderElectionLockPollingInterval;
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

    public boolean isCloud() {
        return isCloud;
    }

    public boolean getAutoRestartInputs() {
        return autoRestartInputs;
    }

    public boolean runMigrations() {
        return runMigrations;
    }

    public boolean ignoreMigrationFailures() {
        return ignoreMigrationFailures;
    }

    public boolean getSkipPreflightChecks() {
        return skipPreflightChecks;
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

    /**
     * @deprecated Use getStaleLeaderTimeout instead
     */
    @Deprecated
    public int getStaleMasterTimeout() {
        return getStaleLeaderTimeout();
    }

    public int getStaleLeaderTimeout() {
        return staleLeaderTimeout != null ? staleLeaderTimeout : staleMasterTimeout;
    }

    public int getLdapConnectionTimeout() {
        return ldapConnectionTimeout;
    }

    @Deprecated
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

    public int getFailureHandlingQueueCapacity() {
        return failureHandlingQueueCapacity;
    }


    public Duration getFailureHandlingShutdownAwait() {
        return failureHandlingShutdownAwait;
    }

    /**
     * This is needed for backwards compatibility. The setting in TLSProtocolsConfiguration should be used instead.
     */
    @Deprecated
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

    @ValidatorMethod
    public void validateLeaderElectionTimeouts() throws ValidationException {
        if (leaderElectionMode != LeaderElectionMode.AUTOMATIC) {
            return;
        }
        if (lockServiceLockTTL.compareTo(MongoLockService.MIN_LOCK_TTL) < 0) {
            throw new ValidationException("The minimum valid \"lock_service_lock_ttl\" is 60 seconds");
        }
        if (leaderElectionLockPollingInterval.compareTo(java.time.Duration.ofSeconds(1)) < 0) {
            throw new ValidationException("The minimum valid \"leader_election_lock_polling_interval\" is 1 second");
        }
        if (lockServiceLockTTL.compareTo(leaderElectionLockPollingInterval) < 1) {
            throw new ValidationException("The \"leader_election_lock_polling_interval\" needs to be greater than the \"lock_service_lock_ttl\"!");
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
            throw new ValidationException("Node ID file at path " + path + " isn't " + b + ". Please specify the correct path or change the permissions");
        }
    }
}

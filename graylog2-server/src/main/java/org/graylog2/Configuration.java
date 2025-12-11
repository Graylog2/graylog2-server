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
import com.github.joschi.jadconfig.documentation.DocumentationSection;
import com.github.joschi.jadconfig.util.Duration;
import com.github.joschi.jadconfig.validators.PositiveDurationValidator;
import com.github.joschi.jadconfig.validators.PositiveIntegerValidator;
import com.github.joschi.jadconfig.validators.PositiveLongValidator;
import com.github.joschi.jadconfig.validators.StringNotBlankValidator;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.graylog.plugins.views.search.engine.suggestions.FieldValueSuggestionMode;
import org.graylog.plugins.views.search.engine.suggestions.FieldValueSuggestionModeConverter;
import org.graylog.security.certutil.CaConfiguration;
import org.graylog2.bindings.NamedBindingOverride;
import org.graylog2.cluster.leader.AutomaticLeaderElectionService;
import org.graylog2.cluster.leader.LeaderElectionMode;
import org.graylog2.cluster.leader.LeaderElectionService;
import org.graylog2.cluster.lock.MongoLockService;
import com.github.joschi.jadconfig.documentation.Documentation;
import org.graylog2.configuration.DocumentationConstants;
import org.graylog2.configuration.converters.JavaDurationConverter;
import org.graylog2.notifications.Notification;
import org.graylog2.outputs.BatchSizeConfig;
import org.graylog2.plugin.Tools;
import org.graylog2.security.hashing.PBKDF2PasswordAlgorithm;
import org.graylog2.security.realm.RootAccountRealm;
import org.graylog2.utilities.IPSubnetConverter;
import org.graylog2.utilities.IpSubnet;
import org.joda.time.DateTimeZone;
import org.joda.time.Period;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * Helper class to hold configuration of Graylog
 */
@SuppressWarnings("FieldMayBeFinal")
@DocumentationSection(heading = "GRAYLOG CONFIGURATION FILE", description = DocumentationConstants.SERVER_DOCUMENTATION_DESCRIPTION)
public class Configuration extends CaConfiguration implements CommonNodeConfiguration {
    public static final String SAFE_CLASSES = "safe_classes";

    public static final String CONTENT_PACKS_DIR = "content_packs_dir";
    private static final String NODE_ID_FILE = "node_id_file";
    /**
     * Deprecated! Use isLeader() instead.
     */
    @Documentation(visible = false)
    @Parameter(value = "is_master")
    private boolean isMaster = true;

    @Documentation("""
            If set to "true", Graylog will periodically investigate indices to figure out which fields are used in which streams.
            It will make field list in Graylog interface show only fields used in selected streams, but can decrease system performance,
            especially on systems with great number of streams and fields.
            """)
    @Parameter(value = "stream_aware_field_types")
    private boolean streamAwareFieldTypes = false;

    /**
     * Used for initializing static leader election. You shouldn't use this for other purposes, but if you must, don't
     * use @{@link jakarta.inject.Named} injection but the getter isLeader() instead.
     **/
    @Documentation("""
            If you are running more than one instances of Graylog server you have to select one of these
            instances as leader. The leader will perform some periodical tasks that non-leaders won't perform.
            """)
    @Parameter(value = "is_leader")
    private Boolean isLeader;

    @Documentation("""
            You MUST set a secret to secure/pepper the stored user passwords here. Use at least 64 characters.
            Generate one by using for example: pwgen -N 1 -s 96
            ATTENTION: This value must be the same on all Graylog nodes in the cluster.
            Changing this value after installation will render all user sessions and encrypted values in the database invalid. (e.g. encrypted access tokens)
            """)
    @Parameter(value = "password_secret", required = true, validators = StringNotBlankValidator.class)
    private String passwordSecret;

    @Documentation("""
            Batch size for the Elasticsearch output. This is the maximum accumulated size of messages that are written to
            Elasticsearch in a batch call. If the configured batch size has not been reached within output_flush_interval seconds,
            everything that is available will be flushed at once.
            Each output buffer processor has to keep an entire batch of messages in memory until it has been sent to
            Elasticsearch, so increasing this value will also increase the memory requirements of the Graylog server.
            Batch sizes can be specified in data units (e.g. bytes, kilobytes, megabytes) or as an absolute number of messages.
            Example: output_batch_size = 10mb
            """)
    @Parameter(value = "output_batch_size", required = true, converter = BatchSizeConfig.Converter.class,
               validators = BatchSizeConfig.Validator.class)
    private BatchSizeConfig outputBatchSize = BatchSizeConfig.forCount(500);

    @Documentation("""
            Flush interval (in seconds) for the Elasticsearch output. This is the maximum amount of time between two
            batches of messages written to Elasticsearch. It is only effective at all if your minimum number of messages
            for this time period is less than output_batch_size * outputbuffer_processors.
            """)
    @Parameter(value = "output_flush_interval", required = true, validators = PositiveIntegerValidator.class)
    private int outputFlushInterval = 1;

    @Documentation("""
            Number of output buffer processors running in parallel.
            By default, the value will be determined automatically based on the number of CPU cores available to the JVM, using
            the formula (<#cores> * 0.162 + 0.625) rounded to the nearest integer.
            Set this value explicitly to override the dynamically calculated value. Try raising the number if your buffers are
            filling up.
            """)
    @Parameter(value = "outputbuffer_processors", required = true, validators = PositiveIntegerValidator.class)
    private int outputBufferProcessors = defaultNumberOfOutputBufferProcessors();

    @Documentation("""
            The size of the thread pool in the output buffer processor.
            Default: 3
            """)
    @Parameter(value = "outputbuffer_processor_threads_core_pool_size", required = true, validators = PositiveIntegerValidator.class)
    private int outputBufferProcessorThreadsCorePoolSize = 3;

    @Documentation("""
            The auto-generated node ID will be stored in this file and read after restarts. It is a good idea
            to use an absolute file path here if you are starting Graylog server from init scripts or similar.
            """)
    @Parameter(value = NODE_ID_FILE, validators = NodeIdFileValidator.class)
    private String nodeIdFile;

    @Documentation("The default root user is named 'admin'")
    @Parameter(value = "root_username")
    private String rootUsername = "admin";

    // Required unless "root-user" is deactivated in the "deactivated_builtin_authentication_providers" setting
    @Documentation("""
            You MUST specify a hash password for the root user (which you only need to initially set up the
            system and in case you lose connectivity to your authentication backend)
            This password cannot be changed using the API or via the web interface. If you need to change it,
            modify it in this file.
            Create one by using for example: echo -n yourpassword | sha256sum
            and put the resulting hash value into the following line
            """)
    @Parameter(value = "root_password_sha2")
    private String rootPasswordSha2;

    @Documentation("""
            The time zone setting of the root user. See http://www.joda.org/joda-time/timezones.html for a list of valid time zones.
            Default is UTC
            """)
    @Parameter(value = "root_timezone")
    private DateTimeZone rootTimeZone = DateTimeZone.UTC;

    @Documentation("""
            The email address of the root user.
            Default is empty
            """)
    @Parameter(value = "root_email")
    private String rootEmail = "";

    @Documentation("""
            Do you want to allow searches with leading wildcards? This can be extremely resource hungry and should only
            be enabled with care. See also: https://docs.graylog.org/docs/query-language
            """)
    @Parameter(value = "allow_leading_wildcard_searches")
    private boolean allowLeadingWildcardSearches = false;

    @Documentation("""
            Do you want to allow searches to be highlighted? Depending on the size of your messages this can be memory hungry and
            should only be enabled after making sure your Elasticsearch cluster has enough memory.
            """)
    @Parameter(value = "allow_highlighting")
    private boolean allowHighlighting = false;

    @Documentation("""
            How many seconds to wait between marking node as DEAD for possible load balancers and starting the actual
            shutdown process. Set to 0 if you have no status checking load balancers in front.
            """)
    @Parameter(value = "lb_recognition_period_seconds", validators = PositiveIntegerValidator.class)
    private int loadBalancerRecognitionPeriodSeconds = 3;

    @Documentation("""
            Journal usage percentage that triggers requesting throttling for this server node from load balancers. The feature is
            disabled if not set.
            """)
    @Parameter(value = "lb_throttle_threshold_percentage", validators = PositiveIntegerValidator.class)
    private int loadBalancerThrottleThresholdPercentage = 100;

    @Documentation("""
            Every message is matched against the configured streams and it can happen that a stream contains rules which
            take an unusual amount of time to run, for example if its using regular expressions that perform excessive backtracking.
            This will impact the processing of the entire server. To keep such misbehaving stream rules from impacting other
            streams, Graylog limits the execution time for each stream.
            The default values are noted below, the timeout is in milliseconds.
            If the stream matching for one stream took longer than the timeout value, and this happened more than "max_faults" times
            that stream is disabled and a notification is shown in the web interface.
            """)
    @Parameter(value = "stream_processing_timeout", validators = PositiveLongValidator.class)
    private long streamProcessingTimeout = 2000;

    @Documentation("tbd")
    @Parameter(value = "stream_processing_max_faults", validators = PositiveIntegerValidator.class)
    private int streamProcessingMaxFaults = 3;

    @Documentation("""
            Since 0.21 the Graylog server supports pluggable output modules. This means a single message can be written to multiple
            outputs. The next setting defines the timeout for a single output module, including the default output module where all
            messages end up.

            Time in milliseconds to wait for all message outputs to finish writing a single message.
            """)
    @Parameter(value = "output_module_timeout", validators = PositiveLongValidator.class)
    private long outputModuleTimeout = 10000;

    @Documentation("""
            As stream outputs are loaded only on demand, an output which is failing to initialize will be tried over and
            over again. To prevent this, the following configuration options define after how many faults (output_fault_count_threshold)
            an output will not be tried again for an also configurable amount of seconds (see output_fault_penalty_seconds).
            """)
    @Parameter(value = "output_fault_count_threshold", validators = PositiveLongValidator.class)
    private long outputFaultCountThreshold = 5;

    @Documentation("""
            As stream outputs are loaded only on demand, an output which is failing to initialize will be tried over and
            over again. To prevent this, the following configuration options define after how many faults (see output_fault_count_threshold)
            an output will not be tried again for an also configurable amount of seconds (output_fault_penalty_seconds).
            """)
    @Parameter(value = "output_fault_penalty_seconds", validators = PositiveLongValidator.class)
    private long outputFaultPenaltySeconds = 30;

    /**
     * Deprecated! Use staleLeaderTimeout instead
     */
    @Documentation("tbd")
    @Parameter(value = "stale_master_timeout", validators = PositiveIntegerValidator.class)
    private int staleMasterTimeout = 2000;

    /**
     * Don't use @{@link jakarta.inject.Named} injection but the getter getStaleLeaderTimeout() instead.
     **/
    @Documentation("Time in milliseconds after which a detected stale leader node is being rechecked on startup.")
    @Parameter(value = "stale_leader_timeout", validators = PositiveIntegerValidator.class)
    private Integer staleLeaderTimeout;

    @Documentation("tbd")
    @Parameter(value = "static_leader_timeout", converter = JavaDurationConverter.class)
    private java.time.Duration staticLeaderTimeout = java.time.Duration.of(60, java.time.temporal.ChronoUnit.SECONDS);

    @Documentation("Connection timeout for a configured LDAP server (e. g. ActiveDirectory) in milliseconds.")
    @Parameter(value = "ldap_connection_timeout", validators = PositiveIntegerValidator.class)
    private int ldapConnectionTimeout = 2000;

    @Documentation("tbd")
    @Parameter(value = "alert_check_interval", validators = PositiveIntegerValidator.class)
    @Deprecated
    private int alertCheckInterval = 60;

    @Documentation("tbd")
    @Parameter(value = "default_message_output_class")
    private String defaultMessageOutputClass = "";

    @Documentation("The default cache time for dashboard widgets. (Default: 10 seconds, minimum: 1 second)")
    @Parameter(value = "dashboard_widget_default_cache_time", validators = PositiveDurationValidator.class)
    private Duration dashboardWidgetDefaultCacheTime = Duration.seconds(10L);

    @Documentation("tbd")
    @Parameter(value = "user_password_default_algorithm")
    private String userPasswordDefaultAlgorithm = "bcrypt";

    @Documentation("tbd")
    @Parameter(value = "user_password_bcrypt_salt_size", validators = PositiveIntegerValidator.class)
    private int userPasswordBCryptSaltSize = 10;

    @Documentation("tbd")
    @Parameter(value = "user_password_pbkdf2_iterations", validators = PositiveIntegerValidator.class)
    private int userPasswordPbkdf2Iterations = PBKDF2PasswordAlgorithm.DEFAULT_ITERATIONS;

    @Documentation("Automatically load content packs in \"content_packs_dir\" on the first start of Graylog.")
    @Parameter(value = "content_packs_loader_enabled")
    private boolean contentPacksLoaderEnabled = false;

    @Documentation("""
            The directory which contains content packs which should be loaded on the first start of Graylog.
            Default: <data_dir>/contentpacks
            """)
    @Parameter(value = CONTENT_PACKS_DIR)
    private Path contentPacksDir;

    @Documentation("""
            A comma-separated list of content packs (files in "content_packs_dir") which should be applied on
            the first start of Graylog.
            Default: empty
            """)
    @Parameter(value = "content_packs_auto_install", converter = TrimmedStringSetConverter.class)
    private Set<String> contentPacksAutoInstall = Collections.emptySet();

    @Documentation("""
            Time interval for index range information cleanups. This setting defines how often stale index range information
            is being purged from the database.
            Default: 1h
            """)
    @Parameter(value = "index_ranges_cleanup_interval", validators = PositiveDurationValidator.class)
    private Duration indexRangesCleanupInterval = Duration.hours(1L);

    @Documentation("""
            Comma separated list of trusted proxies that are allowed to set the client address with X-Forwarded-For
            header. May be subnets, or hosts.
            """)
    @Parameter(value = "trusted_proxies", converter = IPSubnetConverter.class)
    private Set<IpSubnet> trustedProxies = Collections.emptySet();

    @Documentation("tbd")
    @Parameter(value = "deactivated_builtin_authentication_providers", converter = StringSetConverter.class)
    private Set<String> deactivatedBuiltinAuthenticationProviders = Collections.emptySet();

    // This is needed for backwards compatibility. The setting in TLSProtocolsConfiguration should be used instead.
    @Documentation("""
            The allowed TLS protocols for system wide TLS enabled servers. (e.g. message inputs, http interface)
            Setting this to an empty value, leaves it up to system libraries and the used JDK to chose a default.
            Default: TLSv1.2,TLSv1.3  (might be automatically adjusted to protocols supported by the JDK)
            """)
    @Parameter(value = "enabled_tls_protocols", converter = StringSetConverter.class)
    private Set<String> enabledTlsProtocols = null;

    @Documentation("tbd")
    @Parameter(value = "failure_handling_queue_capacity", validators = {PositiveIntegerValidator.class})
    private int failureHandlingQueueCapacity = 1000;

    @Documentation("tbd")
    @Parameter(value = "failure_handling_shutdown_await", validators = {PositiveDurationValidator.class})
    private Duration failureHandlingShutdownAwait = Duration.milliseconds(3000);

    @Documentation("tbd")
    @Parameter(value = "is_cloud")
    private boolean isCloud = false;

    @Documentation("Manually stopped inputs are no longer auto-restarted. To re-enable the previous behavior, set auto_restart_inputs to true.")
    @Parameter(value = "auto_restart_inputs")
    private boolean autoRestartInputs = false;

    @Documentation("tbd")
    @Parameter(value = "run_migrations")
    private boolean runMigrations = true;

    @Documentation("""
            Ignore any exceptions encountered when running migrations
            Use with caution - skipping failing migrations may result in an inconsistent DB state.
            Default: false
            """)
    @Parameter(value = "ignore_migration_failures")
    private boolean ignoreMigrationFailures = false;

    @Documentation("""
            Do not perform any preflight checks when starting Graylog
            Default: false
            """)
    @Parameter(value = "skip_preflight_checks")
    private boolean skipPreflightChecks = false;

    @Documentation("tbd")
    @Parameter(value = "enable_preflight_web")
    private boolean enablePreflightWeb = false;

    @Documentation("tbd")
    @Parameter(value = "preflight_web_password")
    private String preflightWebPassword = null;

    @Documentation("tbd")
    @Parameter(value = "query_latency_monitoring_enabled")
    private boolean queryLatencyMonitoringEnabled = false;

    @Documentation("tbd")
    @Parameter(value = "query_latency_monitoring_window_size")
    private int queryLatencyMonitoringWindowSize = 0;

    @Documentation("tbd")
    @Parameter(value = "leader_election_mode", converter = LeaderElectionMode.Converter.class)
    private LeaderElectionMode leaderElectionMode = LeaderElectionMode.STATIC;

    @Documentation("tbd")
    @Parameter(value = "leader_election_lock_polling_interval", converter = JavaDurationConverter.class)
    private java.time.Duration leaderElectionLockPollingInterval = AutomaticLeaderElectionService.DEFAULT_POLLING_INTERVAL;

    @Documentation("tbd")
    @Parameter(value = "lock_service_lock_ttl", converter = JavaDurationConverter.class)
    private java.time.Duration lockServiceLockTTL = MongoLockService.MIN_LOCK_TTL;

    @Documentation("""
            Comma-separated list of notification types which should not emit a system event.
            Default: SIDECAR_STATUS_UNKNOWN which would create a new event whenever the status of a sidecar becomes "Unknown"
            """)
    @Parameter(value = "system_event_excluded_types", converter = TrimmedStringSetConverter.class)
    private Set<String> systemEventExcludedTypes = Sets.newHashSet(Notification.Type.SIDECAR_STATUS_UNKNOWN.name());

    @Documentation("tbd")
    @Parameter(value = "datanode_proxy_api_allowlist")
    private boolean datanodeProxyAPIAllowlist = true;

    @Documentation("tbd")
    @Parameter(value = "minimum_auto_refresh_interval", required = true)
    private Period minimumAutoRefreshInterval = Period.seconds(1);

    /**
     * Classes considered safe to load by name. A set of prefixes matched against the fully qualified class name.
     */
    @Documentation("tbd")
    @Parameter(value = SAFE_CLASSES, converter = StringSetConverter.class, validators = SafeClassesValidator.class)
    private Set<String> safeClasses = Set.of("org.graylog.", "org.graylog2.");

    @Documentation("""
            Sets field value suggestion mode. The possible values are:
              1. "off" - field value suggestions are turned off
              2. "textual_only" - field values are suggested only for textual fields
              3. "on" (default) - field values are suggested for all field types, even the types where suggestions are inefficient performance-wise
            """)
    @Parameter(value = "field_value_suggestion_mode", required = true, converter = FieldValueSuggestionModeConverter.class)
    private FieldValueSuggestionMode fieldValueSuggestionMode = FieldValueSuggestionMode.ON;

    @Documentation("""
            The size of the thread pool that executes search jobs for indexed data. (Data Node/OpenSearch)
            WARNING: This configuration setting should only be changed if you are certain of what you are doing.
                     Modifying this setting without proper knowledge may lead to unexpected behavior or system
                     instability. Proceed with caution.
            Default: 4
            """)
    @Parameter(value = "search_query_engine_indexer_jobs_pool_size", validators = PositiveIntegerValidator.class)
    private int searchQueryEngineIndexerJobsPoolSize = 4;

    @Documentation("""
            The queue size for the thread pool that executes search jobs for indexed data. (Data Node/OpenSearch)
            A value of "0" means that the queue is unbounded.
            WARNING: This configuration setting should only be changed if you are certain of what you are doing.
                     Modifying this setting without proper knowledge may lead to unexpected behavior or system
                     instability. Proceed with caution.
            Default: 0
            """)
    @Parameter("search_query_engine_indexer_jobs_queue_size")
    private int searchQueryEngineIndexerJobsQueueSize = 0;

    @Documentation("""
            The size of the thread pool that executes search jobs for data in Data Lake.
            WARNING: This configuration setting should only be changed if you are certain of what you are doing.
                     Modifying this setting without proper knowledge may lead to unexpected behavior or system
                     instability. Proceed with caution.
            Default: 4
            """)
    @Parameter(value = "search_query_engine_data_lake_jobs_pool_size", validators = PositiveIntegerValidator.class)
    private int searchQueryEngineDataLakeJobsPoolSize = 4;

    @Documentation("""
            The queue size for the thread pool that executes search jobs for data in Data Lake.
            A value of "0" means that the queue is unbounded.
            WARNING: This configuration setting should only be changed if you are certain of what you are doing.
                     Modifying this setting without proper knowledge may lead to unexpected behavior or system
                     instability. Proceed with caution.
            Default: 0
            """)
    @Parameter("search_query_engine_data_lake_jobs_queue_size")
    private int searchQueryEngineDataLakeJobsQueueSize = 0;

    @Documentation("""
            Enabling this parameter will activate automatic security configuration. Graylog server will
            set a default 30-day automatic certificate renewal policy and create a self-signed CA. This CA
            will be used to sign certificates for SSL communication between the server and datanodes.
            """)
    @Parameter(value = "selfsigned_startup")
    private boolean selfsignedStartup = false;

    public static final String INSTALL_HTTP_CONNECTION_TIMEOUT = "install_http_connection_timeout";
    public static final String INSTALL_OUTPUT_BUFFER_DRAINING_INTERVAL = "install_output_buffer_drain_interval";
    public static final String INSTALL_OUTPUT_BUFFER_DRAINING_MAX_RETRIES = "install_output_buffer_max_retries";

    private static final int DEFAULT_INSTALL_RETRIES = 150;
    private static final Duration DEFAULT_INSTALL_SECONDS = Duration.seconds(2);

    @Documentation("tbd")
    @Parameter(value = INSTALL_HTTP_CONNECTION_TIMEOUT, validators = PositiveDurationValidator.class)
    private Duration installHttpConnectionTimeout = Duration.seconds(10L);

    @Documentation("tbd")
    @Parameter(value = INSTALL_OUTPUT_BUFFER_DRAINING_INTERVAL, validators = PositiveDurationValidator.class)
    private Duration installOutputBufferDrainingInterval = DEFAULT_INSTALL_SECONDS;

    // The maximum number of times to check if buffers have drained during Illuminate restarts on all
    // nodes before giving up
    @Documentation("tbd")
    @Parameter(value = INSTALL_OUTPUT_BUFFER_DRAINING_MAX_RETRIES, validators = PositiveIntegerValidator.class)
    private int installOutputBufferDrainingMaxRetries = DEFAULT_INSTALL_RETRIES;

    @Documentation("tbd")
    @Parameter(value = "global_inputs_only")
    private boolean globalInputsOnly = false;

    @Documentation("tbd")
    @Parameter(value = "max_event_age", converter = JavaDurationConverter.class)
    private java.time.Duration maxEventAge = java.time.Duration.ofDays(1L);

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

    public Set<String> getSystemEventExcludedTypes() {
        return systemEventExcludedTypes;
    }

    public java.time.Duration getLeaderElectionLockPollingInterval() {
        return leaderElectionLockPollingInterval;
    }

    public String getPasswordSecret() {
        return passwordSecret.trim();
    }

    public BatchSizeConfig getOutputBatchSize() {
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

    @NamedBindingOverride(value = NODE_ID_FILE)
    public String getNodeIdFile() {
        return Optional.ofNullable(nodeIdFile).orElse(getDataDir().resolve("node_id").toString());
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

    public java.time.Duration getStaticLeaderTimeout() {
        return staticLeaderTimeout;
    }

    public int getLdapConnectionTimeout() {
        return ldapConnectionTimeout;
    }

    @Deprecated
    public int getAlertCheckInterval() {
        return alertCheckInterval;
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

    @NamedBindingOverride(value = CONTENT_PACKS_DIR)
    public Path getContentPacksDir() {
        return Optional.ofNullable(contentPacksDir).orElse(getDataDir().resolve("contentpacks"));
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

    public Period getMinimumAutoRefreshInterval() {
        return minimumAutoRefreshInterval;
    }

    public Set<String> getSafeClasses() {
        return safeClasses;
    }

    public FieldValueSuggestionMode getFieldValueSuggestionMode() {
        return fieldValueSuggestionMode;
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

    public boolean enablePreflightWebserver() {
        return enablePreflightWeb;
    }

    public boolean isQueryLatencyMonitoringEnabled() {
        return queryLatencyMonitoringEnabled;
    }

    public int getQueryLatencyMonitoringWindowSize() {
        return queryLatencyMonitoringWindowSize;
    }

    public boolean selfsignedStartupEnabled() {
        return selfsignedStartup;
    }

    public int searchQueryEngineIndexerJobsPoolSize() {
        return searchQueryEngineIndexerJobsPoolSize;
    }

    public int searchQueryEngineIndexerJobsQueueSize() {
        return searchQueryEngineIndexerJobsQueueSize;
    }

    public int searchQueryEngineDataLakeJobsPoolSize() {
        return searchQueryEngineDataLakeJobsPoolSize;
    }

    public int searchQueryEngineDataLakeJobsQueueSize() {
        return searchQueryEngineDataLakeJobsQueueSize;
    }

    public String getPreflightWebPassword() {
        return preflightWebPassword;
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

    public static class SafeClassesValidator implements Validator<Set<String>> {
        @Override
        public void validate(String name, Set<String> set) throws ValidationException {
            if (set.isEmpty()) {
                throw new ValidationException(f("\"%s\" must not be empty. Please specify a comma-separated list of " +
                        "fully-qualified class name prefixes.", name));
            }
            if (set.stream().anyMatch(StringUtils::isBlank)) {
                throw new ValidationException(f("\"%s\" must only contain non-empty class name prefixes.", name));
            }
        }
    }

    /**
     * Calculate the default number of output buffer processors as a linear function of available CPU cores.
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
     *         <td>4</td><td>1</td>
     *     </tr>
     *     <tr>
     *         <td>8</td><td>2</td>
     *     </tr>
     *     <tr>
     *         <td>12</td><td>3</td>
     *     </tr>
     *     <tr>
     *         <td>16</td><td>3</td>
     *     </tr>
     * </table>
     */
    private static int defaultNumberOfOutputBufferProcessors() {
        return Math.round(Tools.availableProcessors() * 0.162f + 0.625f);
    }

    @Override
    public boolean withPlugins() {
        return true;
    }

    @Override
    public boolean withInputs() {
        return true;
    }

    public boolean isGlobalInputsOnly() {
        return globalInputsOnly;
    }
}

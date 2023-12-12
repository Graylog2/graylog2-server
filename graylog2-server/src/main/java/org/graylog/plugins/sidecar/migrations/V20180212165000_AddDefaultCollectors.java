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
package org.graylog.plugins.sidecar.migrations;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog.plugins.sidecar.rest.models.Collector;
import org.graylog.plugins.sidecar.rest.models.Configuration;
import org.graylog.plugins.sidecar.rest.models.ConfigurationVariable;
import org.graylog.plugins.sidecar.services.CollectorService;
import org.graylog.plugins.sidecar.services.ConfigurationService;
import org.graylog.plugins.sidecar.services.ConfigurationVariableService;
import org.graylog2.configuration.HttpConfiguration;
import org.graylog2.database.MongoConnection;
import org.graylog2.migrations.Migration;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.exists;
import static org.graylog2.shared.utilities.StringUtils.f;

public class V20180212165000_AddDefaultCollectors extends Migration {
    public static final String BEATS_PREAMBEL = """
            # Needed for Graylog
            fields_under_root: true
            fields.collector_node_id: ${sidecar.nodeName}
            fields.gl2_source_collector: ${sidecar.nodeId}

            """;
    public static final String OS_FREEBSD = "freebsd";
    public static final String OS_LINUX = "linux";
    public static final String OS_DARWIN = "darwin";
    public static final String OS_WINDOWS = "windows";
    private static final Logger LOG = LoggerFactory.getLogger(V20180212165000_AddDefaultCollectors.class);
    private final CollectorService collectorService;
    private final ConfigurationVariableService configurationVariableService;
    private final MongoCollection<Document> collection;
    private final URI httpExternalUri;
    private final ConfigurationService configurationService;

    private final ClusterConfigService clusterConfigService;
    private MigrationState migrationState;
    private MigrationState updatedMigrationState;

    @Inject
    public V20180212165000_AddDefaultCollectors(HttpConfiguration httpConfiguration,
                                                CollectorService collectorService,
                                                ConfigurationVariableService configurationVariableService,
                                                ConfigurationService configurationService,
                                                MongoConnection mongoConnection,
                                                ClusterConfigService clusterConfigService) {
        this.httpExternalUri = httpConfiguration.getHttpExternalUri();
        this.collectorService = collectorService;
        this.configurationVariableService = configurationVariableService;
        this.configurationService = configurationService;
        this.collection = mongoConnection.getMongoDatabase().getCollection(CollectorService.COLLECTION_NAME);
        this.clusterConfigService = clusterConfigService;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2018-02-12T16:50:00Z");
    }

    @Override
    public void upgrade() {
        migrationState = clusterConfigService.getOrDefault(MigrationState.class, MigrationState.createEmpty());
        updatedMigrationState = migrationState;

        removeConfigPath();
        ensureConfigurationVariable("graylog_host", "Graylog Host.", httpExternalUri.getHost());

        ensureFilebeatCollectorsAndConfig();
        ensureAuditbeatCollectorsAndConfig();
        ensureWinlogbeatCollectorsAndConfig();
        ensureNxLogCollectors();

        if (!updatedMigrationState.equals(migrationState)) {
            clusterConfigService.write(updatedMigrationState);
        }
    }

    private void ensureFilebeatCollectorsAndConfig() {

        StringBuilder filebeatConfigBuilder = new StringBuilder(f("""
                        %s
                        output.logstash:
                           hosts: ["${user.graylog_host}:5044"]
                        path:
                           data: ${sidecar.spoolDir!\"/var/lib/graylog-sidecar/collectors/filebeat\"}/data
                           logs: ${sidecar.spoolDir!\"/var/lib/graylog-sidecar/collectors/filebeat\"}/log

                        filebeat.inputs:
                        """,
                BEATS_PREAMBEL));

        String apacheConfigType = """
                    - type: filestream
                      id: apache-filestream
                      enabled: true
                      %s
                      fields_under_root: true
                      fields:
                          event_source_product: apache_httpd""";

        ensureFilebeatCollector(OS_LINUX, "/usr/lib/graylog-sidecar/filebeat", filebeatConfigBuilder
                .append("""

                        - type: filestream
                          id: snort-filestream
                          enabled: true
                          paths:
                            - /var/log/snort/alert_json.txt
                            - /var/log/snort/appid-output.json
                          parsers:
                            - ndjson:
                                target: "snort3"
                                add_error_key: true
                                overwrite_keys: true
                          fields:
                            event_source_product: snort3
                        """)
                .append("""

                        - type: filestream
                          id: zeek-filestream
                          enabled: true
                          paths:
                            - /opt/zeek/logs/current
                          parsers:
                            - ndjson:
                                target: "zeek"
                                add_error_key: true
                                overwrite_keys: true
                          fields:
                            event_source_product: zeek
                        """)
                .append(f(apacheConfigType, """
                        paths:
                          - /var/log/apache2/access.log
                          - /var/log/apache2/error.log
                          - /var/log/httpd/access_log
                          - /var/log/httpd/error_log
                        """))
                .toString()
        ).ifPresent(collector -> ensureDefaultConfiguration("filebeat-linux-default", collector));

        ensureFilebeatCollector(OS_FREEBSD, "/usr/share/filebeat/bin/filebeat", filebeatConfigBuilder
                .append(f(apacheConfigType, """
                        paths:
                          - /var/log/httpd-access.log
                          - /var/log/httpd-error.log
                        """))
                .toString()
        ).ifPresent(collector -> ensureDefaultConfiguration("filebeat-freebsd-default", collector));

        ensureFilebeatCollector(OS_DARWIN, "/usr/share/filebeat/bin/filebeat", filebeatConfigBuilder
                .append(f(apacheConfigType, """
                        paths:
                          - /etc/httpd/log/access_log
                          - /etc/httpd/log/error_log
                        """))
                .toString()
        ).ifPresent(collector -> ensureDefaultConfiguration("filebeat-darwin-default", collector));
    }

    private void ensureAuditbeatCollectorsAndConfig() {
        ensureCollector(
                "auditbeat",
                "exec",
                OS_LINUX,
                "/usr/lib/graylog-sidecar/auditbeat",
                "-c  %s",
                "test config -c %s",
                f("""
                                %s
                                output.logstash:
                                   hosts: ["${user.graylog_host}:5044"]
                                path:
                                   data: ${sidecar.spoolDir!\"/var/lib/graylog-sidecar/collectors/auditbeat\"}/data
                                   logs: ${sidecar.spoolDir!\"/var/lib/graylog-sidecar/collectors/auditbeat\"}/log
                                fields:
                                  event_source_product: linux_auditbeat

                                # You can find the full configuration reference here:
                                # https://www.elastic.co/guide/en/beats/auditbeat/index.html

                                # =========================== Modules configuration ============================
                                auditbeat.modules:

                                # The auditd module collects events from the audit framework in the Linux kernel.
                                - module: auditd
                                  resolve_ids: true
                                  failure_mode: log
                                  backlog_limit: 8196
                                  rate_limit: 0
                                  include_raw_message: true
                                  include_warnings: true
                                  backpressure_strategy: auto

                                  audit_rules: |
                                    ## Define audit rules here.
                                    ## Create file watches (-w) or syscall audits (-a or -A).

                                    ## If you are on a 64 bit platform, everything should be running
                                    ## in 64 bit mode. This rule will detect any use of the 32 bit syscalls
                                    ## because this might be a sign of someone exploiting a hole in the 32
                                    ## bit API.
                                    -a always,exit -F arch=b32 -S all -F key=32bit-abi

                                    ## Executions.
                                    -a always,exit -F arch=b64 -S execve,execveat -k exec

                                    ## External access (warning: these can be expensive to audit).
                                    -a always,exit -F arch=b64 -S accept,bind,connect -F key=external-access

                                    ## Identity changes.
                                    -w /etc/group -p wa -k identity
                                    -w /etc/passwd -p wa -k identity
                                    -w /etc/gshadow -p wa -k identity
                                    -w /etc/shadow -p wa -k identity

                                    ## Unauthorized access attempts.
                                    -a always,exit -F arch=b32 -S open,creat,truncate,ftruncate,openat,open_by_handle_at -F exit=-EACCES -F auid>=1000 -F auid!=4294967295 -F key=access
                                    -a always,exit -F arch=b32 -S open,creat,truncate,ftruncate,openat,open_by_handle_at -F exit=-EPERM -F auid>=1000 -F auid!=4294967295 -F key=access
                                    -a always,exit -F arch=b64 -S open,creat,truncate,ftruncate,openat,open_by_handle_at -F exit=-EACCES -F auid>=1000 -F auid!=4294967295 -F key=access
                                    -a always,exit -F arch=b64 -S open,creat,truncate,ftruncate,openat,open_by_handle_at -F exit=-EPERM -F auid>=1000 -F auid!=4294967295 -F key=access

                                # The file integrity module sends events when files are changed (created, updated, deleted).
                                # The events contain file metadata and hashes.
                                - module: file_integrity
                                  paths:
                                  - /bin
                                  - /usr/bin
                                  - /sbin
                                  - /usr/sbin
                                  - /etc
                                  - /etc/graylog/server
                                  exclude_files:
                                  - '(?i)\\.sw[nop]$'
                                  - '~$'
                                  - '/\\.git($|/)'
                                  include_files: []
                                  scan_at_start: true
                                  scan_rate_per_sec: 50 MiB
                                  max_file_size: 100 MiB
                                  hash_types: [sha256]
                                  recursive: false""",
                        BEATS_PREAMBEL)
        ).ifPresent(collector -> ensureDefaultConfiguration("auditbeat-linux-default", collector));
    }

    private void ensureWinlogbeatCollectorsAndConfig() {
        ensureCollector(
                "winlogbeat",
                "svc",
                OS_WINDOWS,
                "C:\\Program Files\\Graylog\\sidecar\\winlogbeat.exe",
                "-c \"%s\"",
                "test config -c \"%s\"",
                f("""
                                %s
                                output.logstash:
                                   hosts: ["${user.graylog_host}:5044"]
                                path:
                                  data: ${sidecar.spoolDir!\"C:\\\\Program Files\\\\Graylog\\\\sidecar\\\\cache\\\\winlogbeat\"}\\data
                                  logs: ${sidecar.spoolDir!\"C:\\\\Program Files\\\\Graylog\\\\sidecar\"}\\logs
                                tags:
                                 - windows
                                winlogbeat:
                                  event_logs:
                                   - name: Application
                                     ignore_older: 96h
                                   - name: System
                                     ignore_older: 96h
                                   - name: Security
                                     ignore_older: 96h
                                   - name: Setup
                                     ignore_older: 96h
                                   - name: ForwardedEvents
                                     forwarded: true
                                     ignore_older: 96h
                                   - name: Microsoft-Windows-Windows Defender/Operational
                                     ignore_older: 96h
                                   - name: Microsoft-Windows-Sysmon/Operational
                                     ignore_older: 96h
                                   - name: Microsoft-Windows-TerminalServices-LocalSessionManager/Operational
                                     ignore_older: 96h
                                   - name: Microsoft-Windows-PowerShell/Operational
                                     ignore_older: 96h
                                   - name: windows PowerShell
                                     ignore_older: 96h""",
                        BEATS_PREAMBEL
                )
        ).ifPresent(collector -> ensureDefaultConfiguration("winlogbeat-default", collector));
    }

    private void ensureNxLogCollectors() {
        ensureCollector(
                "nxlog",
                "exec",
                OS_LINUX,
                "/usr/bin/nxlog",
                "-f -c %s",
                "-v -c %s",
                """
                        define ROOT /usr/bin

                        <Extension gelfExt>
                          Module xm_gelf
                          # Avoid truncation of the short_message field to 64 characters.
                          ShortMessageLength 65536
                        </Extension>

                        <Extension syslogExt>
                          Module xm_syslog
                        </Extension>

                        User nxlog
                        Group nxlog

                        Moduledir /usr/lib/nxlog/modules
                        CacheDir ${sidecar.spoolDir!\"/var/spool/nxlog\"}/data
                        PidFile ${sidecar.spoolDir!\"/var/run/nxlog\"}/nxlog.pid
                        LogFile ${sidecar.spoolDir!\"/var/log/nxlog\"}/nxlog.log
                        LogLevel INFO


                        <Input file>
                        \tModule im_file
                        \tFile '/var/log/*.log'
                        \tPollInterval 1
                        \tSavePos\tTrue
                        \tReadFromLast True
                        \tRecursive False
                        \tRenameCheck False
                        \tExec $FileName = file_name(); # Send file name with each message
                        </Input>

                        #<Input syslog-udp>
                        #\tModule im_udp
                        #\tHost 127.0.0.1
                        #\tPort 514
                        #\tExec parse_syslog_bsd();
                        #</Input>

                        <Output gelf>
                        \tModule om_tcp
                        \tHost ${user.graylog_host}
                        \tPort 12201
                        \tOutputType  GELF_TCP
                        \t<Exec>
                        \t  # These fields are needed for Graylog
                        \t  $gl2_source_collector = '${sidecar.nodeId}';
                        \t  $collector_node_id = '${sidecar.nodeName}';
                        \t</Exec>
                        </Output>


                        <Route route-1>
                          Path file => gelf
                        </Route>
                        #<Route route-2>
                        #  Path syslog-udp => gelf
                        #</Route>


                        """
        );
        ensureCollector(
                "nxlog",
                "svc",
                OS_WINDOWS,
                "C:\\Program Files (x86)\\nxlog\\nxlog.exe",
                "-c \"%s\"",
                "-v -f -c \"%s\"",
                """
                        define ROOT ${sidecar.spoolDir!\"C:\\\\Program Files (x86)\"}\\nxlog

                        Moduledir %ROOT%\\modules
                        CacheDir %ROOT%\\data
                        Pidfile %ROOT%\\data\\nxlog.pid
                        SpoolDir %ROOT%\\data
                        LogFile %ROOT%\\data\\nxlog.log
                        LogLevel INFO

                        <Extension logrotate>
                            Module  xm_fileop
                            <Schedule>
                                When    @daily
                                Exec    file_cycle('%ROOT%\\data\\nxlog.log', 7);
                             </Schedule>
                        </Extension>


                        <Extension gelfExt>
                          Module xm_gelf
                          # Avoid truncation of the short_message field to 64 characters.
                          ShortMessageLength 65536
                        </Extension>

                        <Input eventlog>
                                Module im_msvistalog
                                PollInterval 1
                                SavePos True
                                ReadFromLast True
                               \s
                                #Channel System
                                #<QueryXML>
                                #  <QueryList>
                                #   <Query Id='1'>
                                #    <Select Path='Security'>*[System/Level=4]</Select>
                                #    </Query>
                                #  </QueryList>
                                #</QueryXML>
                        </Input>


                        <Input file>
                        \tModule im_file
                        \tFile 'C:\\Windows\\MyLogDir\\\\*.log'
                        \tPollInterval 1
                        \tSavePos\tTrue
                        \tReadFromLast True
                        \tRecursive False
                        \tRenameCheck False
                        \tExec $FileName = file_name(); # Send file name with each message
                        </Input>


                        <Output gelf>
                        \tModule om_tcp
                        \tHost ${user.graylog_host}
                        \tPort 12201
                        \tOutputType  GELF_TCP
                        \t<Exec>
                        \t  # These fields are needed for Graylog
                        \t  $gl2_source_collector = '${sidecar.nodeId}';
                        \t  $collector_node_id = '${sidecar.nodeName}';
                        \t</Exec>
                        </Output>


                        <Route route-1>
                          Path eventlog => gelf
                        </Route>
                        <Route route-2>
                          Path file => gelf
                        </Route>

                        """
        );
    }


    private Optional<Collector> ensureFilebeatCollector(String operatingSystem, String executablePath, String config) {
        return ensureCollector(
                "filebeat",
                "exec",
                operatingSystem,
                executablePath,
                "-c  %s",
                "test config -c %s",
                config
        );
    }

    private void removeConfigPath() {
        final FindIterable<Document> documentsWithConfigPath = collection.find(exists("configuration_path"));
        for (Document document : documentsWithConfigPath) {
            final ObjectId objectId = document.getObjectId("_id");
            document.remove("configuration_path");
            final UpdateResult updateResult = collection.replaceOne(eq("_id", objectId), document);
            if (updateResult.wasAcknowledged()) {
                LOG.debug("Successfully updated document with ID <{}>", objectId);
            } else {
                LOG.error("Failed to update document with ID <{}>", objectId);
            }
        }
    }

    private Optional<Collector> ensureCollector(String collectorName,
                                                String serviceType,
                                                String nodeOperatingSystem,
                                                String executablePath,
                                                String executeParameters,
                                                String validationCommand,
                                                String defaultTemplate) {

        this.updatedMigrationState = updatedMigrationState.withNewDefaultTemplate(collectorName, nodeOperatingSystem, defaultTemplate);

        Collector collector = null;
        try {
            collector = collectorService.findByNameAndOs(collectorName, nodeOperatingSystem);
            if (collector == null) {
                final String msg = "Couldn't find collector '{} on {}' fixing it.";
                LOG.debug(msg, collectorName, nodeOperatingSystem);
                throw new IllegalArgumentException();
            }
            if (!defaultTemplate.equals(collector.defaultTemplate()) &&
                    migrationState.isKnownDefaultTemplate(collectorName, nodeOperatingSystem, collector.defaultTemplate())) {
                LOG.info("{} collector default template on {} is unchanged, updating it.", collectorName, nodeOperatingSystem);
                try {
                    return Optional.of(collectorService.save(
                            collector.toBuilder()
                                    .defaultTemplate(defaultTemplate)
                                    .build()));
                } catch (Exception e) {
                    LOG.error("Can't save collector '{}'!", collectorName, e);
                }
            }
        } catch (IllegalArgumentException ignored) {
            LOG.info("{} collector on {} is missing, adding it.", collectorName, nodeOperatingSystem);
            try {
                return Optional.of(collectorService.save(Collector.create(
                        null,
                        collectorName,
                        serviceType,
                        nodeOperatingSystem,
                        executablePath,
                        executeParameters,
                        validationCommand,
                        defaultTemplate
                )));
            } catch (Exception e) {
                LOG.error("Can't save collector '{}'!", collectorName, e);
            }
        }

        if (collector == null) {
            LOG.error("Unable to access fixed '{}' collector!", collectorName);
            return Optional.empty();
        }


        return Optional.of(collector);
    }

    private void ensureConfigurationVariable(String name, String description, String content) {
        ConfigurationVariable variable = null;
        try {
            variable = configurationVariableService.findByName(name);
            if (variable == null) {
                LOG.debug("Couldn't find sidecar configuration variable '{}' fixing it.", name);
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException ignored) {
            LOG.info("'{}' sidecar configuration variable is missing, adding it.", name);
            try {
                variable = configurationVariableService.save(ConfigurationVariable.create(name, description, content));
            } catch (Exception e) {
                LOG.error("Can't save sidecar configuration variable '{}'!", name, e);
            }
        }

        if (variable == null) {
            LOG.error("Unable to access '{}' sidecar configuration variable!", name);
        }
    }

    private void ensureDefaultConfiguration(String name, Collector collector) {
        Configuration config = null;
        try {
            config = configurationService.findByName(name);
            if (config == null) {
                LOG.debug("Couldn't find sidecar default configuration'{}' fixing it.", name);
                throw new IllegalArgumentException();
            }
            if (!config.template().equals(collector.defaultTemplate()) &&
                    migrationState.isKnownDefaultTemplate(collector.name(), collector.nodeOperatingSystem(), config.template())) {
                LOG.info("Sidecar configuration '{}' still matches a known default. Updating.", name);
                configurationService.save(config.toBuilder().template(collector.defaultTemplate()).build());
            }
        } catch (IllegalArgumentException ignored) {
            LOG.info("'{}' sidecar default configuration is missing, adding it.", name);
            try {
                config = configurationService.save(Configuration.createWithoutId(
                        collector.id(),
                        name,
                        "#ffffff",
                        collector.defaultTemplate(),
                        Set.of("default")));
            } catch (Exception e) {
                LOG.error("Can't save sidecar default configuration '{}'!", name, e);
            }
        }

        if (config == null) {
            LOG.error("Unable to access '{}' sidecar default configuration!", name);
        }
    }

    @JsonAutoDetect
    @AutoValue
    @WithBeanGetter
    public static abstract class MigrationState {

        // Set of prior version CRCs
        // !!! Extending this list does not work. It was persisted after the first migration run.
        private static final Set<CollectorChecksum> OLD_CHECKSUMS = java.util.Set.of(
                // 5.2
                new CollectorChecksum("filebeat", OS_LINUX, 3280545580L),
                new CollectorChecksum("filebeat", OS_DARWIN, 3396210381L),
                new CollectorChecksum("filebeat", OS_FREEBSD, 3013497446L),
                new CollectorChecksum("winlogbeat", OS_WINDOWS, 4009863009L),
                new CollectorChecksum("nxlog", OS_LINUX, 2023247173L),
                new CollectorChecksum("nxlog", OS_WINDOWS, 2491201449L),
                new CollectorChecksum("auditbeat", OS_WINDOWS, 2487909285L),

                // 5.1 & 5.0
                new CollectorChecksum("filebeat", OS_LINUX, 4049210961L),
                new CollectorChecksum("filebeat", OS_DARWIN, 4049210961L),
                new CollectorChecksum("filebeat", OS_FREEBSD, 4049210961L),
                new CollectorChecksum("winlogbeat", OS_WINDOWS, 2306685777L),
                new CollectorChecksum("nxlog", OS_LINUX, 639836274L),
                new CollectorChecksum("nxlog", OS_WINDOWS, 2157898695L),

                // 4.3
                new CollectorChecksum("filebeat", OS_LINUX, 1256873081L),
                new CollectorChecksum("winlogbeat", OS_WINDOWS, 3852098581L),
                new CollectorChecksum("nxlog", OS_LINUX, 3676599312L),
                new CollectorChecksum("nxlog", OS_WINDOWS, 4293222217L)
                // !!! Extending this list does not work. It was persisted after the first migration run.
        );

        @JsonProperty("knownChecksums")
        public abstract Set<CollectorChecksum> knownChecksums();

        public static MigrationState createEmpty() {
            return create(Set.of());
        }

        public MigrationState withNewDefaultTemplate(String collectorName, String platform, String template) {
            var merged = knownChecksums();
            merged.add(checksum(collectorName, platform, template));
            return create(merged);
        }
        @JsonCreator
        public static MigrationState create(@JsonProperty("knownChecksums") Set<CollectorChecksum> knownChecksums) {
            var merged = new HashSet<>(knownChecksums);
            merged.addAll(OLD_CHECKSUMS);

            return new AutoValue_V20180212165000_AddDefaultCollectors_MigrationState(merged);
        }

        @JsonIgnore
        public boolean isKnownDefaultTemplate(String collectorName, String os, @Nullable String template) {
            if (template == null) {
                return false;
            }

            var collectorChecksum = checksum(collectorName, os, template);
            return knownChecksums().contains(collectorChecksum);
        }

        @JsonIgnore
        public static CollectorChecksum checksum(String name, String platform, String template) {
            var bytes = template.getBytes(StandardCharsets.UTF_8);
            Checksum crc32 = new CRC32();
            crc32.update(bytes, 0, bytes.length);
            return new CollectorChecksum(name, platform, crc32.getValue());
        }
    }

    public record CollectorChecksum(@JsonProperty("name") String name,
                                    @JsonProperty("platform") String platform,
                                    @JsonProperty("crc") Long crc) {}
}

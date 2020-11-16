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

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.graylog.plugins.sidecar.rest.models.Collector;
import org.graylog.plugins.sidecar.services.CollectorService;
import org.graylog2.database.MongoConnection;
import org.graylog2.migrations.Migration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.time.ZonedDateTime;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.exists;

public class V20180212165000_AddDefaultCollectors extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20180212165000_AddDefaultCollectors.class);

    private final CollectorService collectorService;
    private final MongoCollection<Document> collection;

    @Inject
    public V20180212165000_AddDefaultCollectors(CollectorService collectorService, MongoConnection mongoConnection) {
        this.collectorService = collectorService;
        this.collection = mongoConnection.getMongoDatabase().getCollection(CollectorService.COLLECTION_NAME);
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2018-02-12T16:50:00Z");
    }

    @Override
    public void upgrade() {

        removeConfigPath();

        final String beatsPreambel =
                "# Needed for Graylog\n" +
                "fields_under_root: true\n" +
                "fields.collector_node_id: ${sidecar.nodeName}\n" +
                "fields.gl2_source_collector: ${sidecar.nodeId}\n\n";

        ensureCollector(
                "filebeat",
                "exec",
                "linux",
                "/usr/share/filebeat/bin/filebeat",
                "-c  %s",
                "test config -c %s",
                beatsPreambel +
                        "filebeat.inputs:\n" +
                        "- input_type: log\n" +
                        "  paths:\n" +
                        "    - /var/log/*.log\n" +
                        "  type: log\n" +
                        "output.logstash:\n" +
                        "   hosts: [\"192.168.1.1:5044\"]\n" +
                        "path:\n" +
                        "  data: /var/lib/graylog-sidecar/collectors/filebeat/data\n" +
                        "  logs: /var/lib/graylog-sidecar/collectors/filebeat/log"
        );
        ensureCollector(
                "winlogbeat",
                "svc",
                "windows",
                "C:\\Program Files\\Graylog\\sidecar\\winlogbeat.exe",
                "-c \"%s\"",
                "test config -c \"%s\"",
                beatsPreambel +
                        "output.logstash:\n" +
                        "   hosts: [\"192.168.1.1:5044\"]\n" +
                        "path:\n" +
                        "  data: C:\\Program Files\\Graylog\\sidecar\\cache\\winlogbeat\\data\n" +
                        "  logs: C:\\Program Files\\Graylog\\sidecar\\logs\n" +
                        "tags:\n" +
                        " - windows\n" +
                        "winlogbeat:\n" +
                        "  event_logs:\n" +
                        "   - name: Application\n" +
                        "   - name: System\n" +
                        "   - name: Security"
        );
        ensureCollector(
                "nxlog",
                "exec",
                "linux",
                "/usr/bin/nxlog",
                "-f -c %s",
                "-v -c %s",
                "define ROOT /usr/bin\n" +
                        "\n" +
                        "<Extension gelfExt>\n" +
                        "  Module xm_gelf\n" +
                        "  # Avoid truncation of the short_message field to 64 characters.\n" +
                        "  ShortMessageLength 65536\n" +
                        "</Extension>\n" +
                        "\n" +
                        "<Extension syslogExt>\n" +
                        "  Module xm_syslog\n" +
                        "</Extension>\n" +
                        "\n" +
                        "User nxlog\n" +
                        "Group nxlog\n" +
                        "\n" +
                        "Moduledir /usr/lib/nxlog/modules\n" +
                        "CacheDir /var/spool/nxlog/data\n" +
                        "PidFile /var/run/nxlog/nxlog.pid\n" +
                        "LogFile /var/log/nxlog/nxlog.log\n" +
                        "LogLevel INFO\n" +
                        "\n" +
                        "\n" +
                        "<Input file>\n" +
                        "\tModule im_file\n" +
                        "\tFile '/var/log/*.log'\n" +
                        "\tPollInterval 1\n" +
                        "\tSavePos\tTrue\n" +
                        "\tReadFromLast True\n" +
                        "\tRecursive False\n" +
                        "\tRenameCheck False\n" +
                        "\tExec $FileName = file_name(); # Send file name with each message\n" +
                        "</Input>\n" +
                        "\n" +
                        "#<Input syslog-udp>\n" +
                        "#\tModule im_udp\n" +
                        "#\tHost 127.0.0.1\n" +
                        "#\tPort 514\n" +
                        "#\tExec parse_syslog_bsd();\n" +
                        "#</Input>\n" +
                        "\n" +
                        "<Output gelf>\n" +
                        "\tModule om_tcp\n" +
                        "\tHost 192.168.1.1\n" +
                        "\tPort 12201\n" +
                        "\tOutputType  GELF_TCP\n" +
                        "\t<Exec>\n" +
                        "\t  # These fields are needed for Graylog\n" +
                        "\t  $gl2_source_collector = '${sidecar.nodeId}';\n" +
                        "\t  $collector_node_id = '${sidecar.nodeName}';\n" +
                        "\t</Exec>\n" +
                        "</Output>\n" +
                        "\n" +
                        "\n" +
                        "<Route route-1>\n" +
                        "  Path file => gelf\n" +
                        "</Route>\n" +
                        "#<Route route-2>\n" +
                        "#  Path syslog-udp => gelf\n" +
                        "#</Route>\n" +
                        "\n" +
                        "\n"
        );
        ensureCollector(
                "nxlog",
                "svc",
                "windows",
                "C:\\Program Files (x86)\\nxlog\\nxlog.exe",
                "-c \"%s\"",
                "-v -f -c \"%s\"",
                "define ROOT C:\\Program Files (x86)\\nxlog\n" +
                        "\n" +
                        "Moduledir %ROOT%\\modules\n" +
                        "CacheDir %ROOT%\\data\n" +
                        "Pidfile %ROOT%\\data\\nxlog.pid\n" +
                        "SpoolDir %ROOT%\\data\n" +
                        "LogFile %ROOT%\\data\\nxlog.log\n" +
                        "LogLevel INFO\n" +
                        "\n" +
                        "<Extension logrotate>\n" +
                        "    Module  xm_fileop\n" +
                        "    <Schedule>\n" +
                        "        When    @daily\n" +
                        "        Exec    file_cycle('%ROOT%\\data\\nxlog.log', 7);\n" +
                        "     </Schedule>\n" +
                        "</Extension>\n" +
                        "\n" +
                        "\n" +
                        "<Extension gelfExt>\n" +
                        "  Module xm_gelf\n" +
                        "  # Avoid truncation of the short_message field to 64 characters.\n" +
                        "  ShortMessageLength 65536\n" +
                        "</Extension>\n" +
                        "\n" +
                        "<Input eventlog>\n" +
                        "        Module im_msvistalog\n" +
                        "        PollInterval 1\n" +
                        "        SavePos True\n" +
                        "        ReadFromLast True\n" +
                        "        \n" +
                        "        #Channel System\n" +
                        "        #<QueryXML>\n" +
                        "        #  <QueryList>\n" +
                        "        #   <Query Id='1'>\n" +
                        "        #    <Select Path='Security'>*[System/Level=4]</Select>\n" +
                        "        #    </Query>\n" +
                        "        #  </QueryList>\n" +
                        "        #</QueryXML>\n" +
                        "</Input>\n" +
                        "\n" +
                        "\n" +
                        "<Input file>\n" +
                        "\tModule im_file\n" +
                        "\tFile 'C:\\Windows\\MyLogDir\\\\*.log'\n" +
                        "\tPollInterval 1\n" +
                        "\tSavePos\tTrue\n" +
                        "\tReadFromLast True\n" +
                        "\tRecursive False\n" +
                        "\tRenameCheck False\n" +
                        "\tExec $FileName = file_name(); # Send file name with each message\n" +
                        "</Input>\n" +
                        "\n" +
                        "\n" +
                        "<Output gelf>\n" +
                        "\tModule om_tcp\n" +
                        "\tHost 192.168.1.1\n" +
                        "\tPort 12201\n" +
                        "\tOutputType  GELF_TCP\n" +
                        "\t<Exec>\n" +
                        "\t  # These fields are needed for Graylog\n" +
                        "\t  $gl2_source_collector = '${sidecar.nodeId}';\n" +
                        "\t  $collector_node_id = '${sidecar.nodeName}';\n" +
                        "\t</Exec>\n" +
                        "</Output>\n" +
                        "\n" +
                        "\n" +
                        "<Route route-1>\n" +
                        "  Path eventlog => gelf\n" +
                        "</Route>\n" +
                        "<Route route-2>\n" +
                        "  Path file => gelf\n" +
                        "</Route>\n" +
                        "\n"
        );
        ensureCollector(
                "filebeat",
                "svc",
                "windows",
                "C:\\Program Files\\Graylog\\sidecar\\filebeat.exe",
                "-c \"%s\"",
                "test config -c \"%s\"",
                beatsPreambel +
                        "output.logstash:\n" +
                        "   hosts: [\"192.168.1.1:5044\"]\n" +
                        "path:\n" +
                        "  data: C:\\Program Files\\Graylog\\sidecar\\cache\\filebeat\\data\n" +
                        "  logs: C:\\Program Files\\Graylog\\sidecar\\logs\n" +
                        "tags:\n" +
                        " - windows\n" +
                        "filebeat.inputs:\n" +
                        "- type: log\n" +
                        "  enabled: true\n" +
                        "  paths:\n" +
                        "    - C:\\logs\\log.log\n"
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

    @Nullable
    private String ensureCollector(String collectorName,
                                   String serviceType,
                                   String nodeOperatingSystem,
                                   String executablePath,
                                   String executeParameters,
                                   String validationCommand,
                                   String defaultTemplate) {
        Collector collector = null;
        try {
            collector = collectorService.findByNameAndOs(collectorName, nodeOperatingSystem);
            if (collector == null) {
                final String msg = "Couldn't find collector '{} on {}' fixing it.";
                LOG.error(msg, collectorName, nodeOperatingSystem);
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException ignored) {
            LOG.info("{} collector on {} is missing, adding it.", collectorName, nodeOperatingSystem);
            final Collector newCollector;
            newCollector = Collector.create(
                    null,
                    collectorName,
                    serviceType,
                    nodeOperatingSystem,
                    executablePath,
                    executeParameters,
                    validationCommand,
                    defaultTemplate
            );
            try {
                return collectorService.save(newCollector).id();
            } catch (Exception e) {
                LOG.error("Can't save collector " + collectorName + ", please restart Graylog to fix this.", e);
            }
        }

        if (collector == null) {
            LOG.error("Unable to access fixed " + collectorName + " collector, please restart Graylog to fix this.");
            return null;
        }

        return collector.id();
    }

}

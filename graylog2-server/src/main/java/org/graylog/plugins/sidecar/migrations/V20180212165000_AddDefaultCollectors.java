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
                "fields.collector_node_id: ${nodeId}\n" +
                "fields.gl2_source_collector: ${nodeName}\n\n";

        ensureCollector(
                "filebeat",
                "exec",
                "linux",
                "/usr/lib/graylog-sidecar/filebeat",
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
                        "  data: /var/cache/graylog-sidecar/filebeat/data\n" +
                        "  logs: /var/log/graylog-sidecar"
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
                "/usr/lib/graylog-sidecar/nxlog",
                "-f -c %s",
                "-v -c %s",
                ""
        );
    }

    private void removeConfigPath() {
        final FindIterable<Document> documentsWithConfigPath = collection.find(exists("configuration_path"));
        for (Document document : documentsWithConfigPath) {
            final ObjectId objectId = document.getObjectId("_id");
            System.out.print( document);
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
            collector = collectorService.findByName(collectorName);
            if (collector == null) {
                final String msg = "Couldn't find collector '" + collectorName + "' fixing it.";
                LOG.error(msg);
                throw new IllegalArgumentException(msg);
            }
        } catch (IllegalArgumentException ignored) {
            LOG.info("{} collector is missing, adding it.", collectorName);
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

package org.graylog.integrations.dbconnector;

import com.codahale.metrics.MetricRegistry;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.assistedinject.Assisted;
import jakarta.inject.Inject;
import org.graylog.integrations.aws.codecs.KinesisRawLogCodec;
import org.graylog.integrations.dbconnector.external.model.DBConnectorEndpoints;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.DropdownField;
import org.graylog2.plugin.configuration.fields.NumberField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import org.graylog2.shared.inputs.InputRegistry;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.graylog.integrations.dbconnector.DBConnectorProperty.INCREMENT;
import static org.graylog.integrations.dbconnector.DBConnectorProperty.TIMESTAMP;

public class DBConnectorInput extends MessageInput {
    private static final Logger LOG = LoggerFactory.getLogger(DBConnectorInput.class);

    public static final String NAME = "DB Connector Logs";
    public static final String TYPE = "org.graylog.integrations.dbconnector.DBConnectorInput";
    public static final String CK_DATABASE_TYPE = "db_type";
    public static final String CK_HOSTNAME = "hostname";
    public static final String CK_PORT = "port";
    public static final String CK_DATABASE_NAME = "database_name";
    public static final String CK_USERNAME = "username";
    public static final String CK_PASSWORD = "password";

    public static final String CK_POLLING_INTERVAL = "polling_interval";
    public static final String CK_POLLING_TIME_UNIT = "polling_time_unit";
    public static final String CK_TABLE_NAME = "table_name";
    public static final String CK_STATE_FIELD_TYPE = "state_field_type";
    public static final String CK_STATE_FILED = "state_field";
    public static final String CK_MONGO_DATABASE_NAME = "mongo_database_name";
    public static final String CK_MONGO_COLLECTION_NAME = "mongo_collection_name";
    public static final String CK_STORE_FULL_MESSAGE = "store_full_message";
    public static final String CK_OVERRIDE_SOURCE = "override_source";
    private final NotificationService notificationService;

    @Inject
    public DBConnectorInput(@Assisted Configuration configuration,
                            MetricRegistry metricRegistry,
                            DBConnectorTransport.Factory transportFactory,
                            LocalMetricRegistry localRegistry,
                            DBConnectorCodec.Factory codecFactory,
                            Config config,
                            Descriptor descriptor,
                            ServerStatus serverStatus,
                            NotificationService notificationService) {

        super(metricRegistry,
                configuration,
                transportFactory.create(configuration),
                localRegistry,
                codecFactory.create(configuration),
                config,
                descriptor,
                serverStatus);

        this.notificationService = notificationService;
    }

    @FactoryClass
    public interface Factory extends MessageInput.Factory<DBConnectorInput> {
        @Override
        DBConnectorInput create(Configuration configuration);

        @Override
        Config getConfig();

        @Override
        Descriptor getDescriptor();
    }

    public static class Descriptor extends MessageInput.Descriptor {
        public Descriptor() {
            super(NAME, false, "");
        }
    }

    @ConfigClass
    public static class Config extends MessageInput.Config {
        @Inject
        public Config(DBConnectorTransport.Factory transport, DBConnectorCodec.Factory codec) {
            super(transport.getConfig(), codec.getConfig());
        }

        @Override
        public ConfigurationRequest combinedRequestedConfiguration() {
            Map<String, String> dbTypes = new HashMap<>();
            for (DBConnectorEndpoints dbType : DBConnectorEndpoints.values()) {
                dbTypes.put(dbType.displayName(), dbType.url());
            }
            ConfigurationRequest request = super.combinedRequestedConfiguration();
            Map<String, String> stateFieldType = new HashMap<>();
            stateFieldType.put(TIMESTAMP, TIMESTAMP);
            stateFieldType.put(INCREMENT, INCREMENT);
            addConnectionFields(request);
            request.addField(new DropdownField(
                    CK_DATABASE_TYPE,
                    "Database Type",
                    DBConnectorEndpoints.ORACLE.displayName(),
                    dbTypes,
                    "The database type which you are configuring.",
                    ConfigurationField.Optional.NOT_OPTIONAL));
            request.addField(new NumberField(
                    CK_POLLING_INTERVAL,
                    "Polling interval",
                    30,
                    "Determines how often Graylog will check for Database  records. The smallest allowable interval is 5 seconds.",
                    ConfigurationField.Optional.NOT_OPTIONAL));
            request.addField(new DropdownField(
                    CK_POLLING_TIME_UNIT,
                    "Interval time unit",
                    TimeUnit.MINUTES.toString(),
                    DropdownField.ValueTemplates.timeUnits(),
                    ConfigurationField.Optional.NOT_OPTIONAL));
            request.addField(new DropdownField(
                    CK_STATE_FIELD_TYPE,
                    "State Field Type",
                    TIMESTAMP,
                    stateFieldType,
                    ConfigurationField.Optional.NOT_OPTIONAL));
            request.addField(new TextField(
                    CK_STATE_FILED,
                    "State Field",
                    "",
                    " Column name based on which the Collector maintains the event state",
                    ConfigurationField.Optional.NOT_OPTIONAL));

            return request;
        }

        public static void addConnectionFields(ConfigurationRequest request) {
            request.addField(new TextField(
                    CK_HOSTNAME,
                    "Hostname",
                    "",
                    "The hostname or IP address of the database server.",
                    ConfigurationField.Optional.NOT_OPTIONAL));

            request.addField(new NumberField(
                    CK_PORT,
                    "Port",
                    1521,
                    "The port on which the database server is listening.",
                    ConfigurationField.Optional.NOT_OPTIONAL));

            request.addField(new TextField(
                    CK_DATABASE_NAME,
                    "Database Name",
                    "",
                    "The name of the target database.",
                    ConfigurationField.Optional.NOT_OPTIONAL));

            request.addField(new TextField(
                    CK_USERNAME,
                    "Username",
                    "",
                    "Username used to authenticate with the database.",
                    ConfigurationField.Optional.NOT_OPTIONAL));

            request.addField(new TextField(
                    CK_PASSWORD,
                    "Password",
                    "",
                    "Password used to authenticate with the database.",
                    ConfigurationField.Optional.NOT_OPTIONAL,
                    TextField.Attribute.IS_PASSWORD));
            request.addField(new TextField(
                    CK_TABLE_NAME,
                    "Table name",
                    "",
                    "The table name from which the data is collected (SQL).",
                    ConfigurationField.Optional.NOT_OPTIONAL));
            request.addField(new TextField(
                    CK_MONGO_COLLECTION_NAME,
                    "Mongo Collection name",
                    "",
                    "The Mongo collection name from which the data is collected.",
                    ConfigurationField.Optional.OPTIONAL));
            request.addField(new TextField(
                    CK_OVERRIDE_SOURCE,
                    "Override Source",
                    "",
                    "The message source is set to hostname|databaseName|tableName. If desired, you may override it with a custom value.",
                    ConfigurationField.Optional.OPTIONAL));

        }
    }

    /* package private */ void fail(Throwable cause) {
        String title = String.format("Input %s is failing to retrieve data", getTitle());
        String errorMsg = String.format(
                "The input has encountered errors while fetching data from DBConnector  servers :: %s",
                cause.getLocalizedMessage());
        LOG.error(errorMsg, cause);
        notificationService.publishIfFirst(
                notificationService.build()
                        .addType(Notification.Type.GENERIC)
                        .addSeverity(Notification.Severity.URGENT)
                        .addTimestamp(DateTime.now())
                        .addNode(getNodeId())
                        .addDetail("title", title)
                        .addDetail("description", errorMsg));
    }
}

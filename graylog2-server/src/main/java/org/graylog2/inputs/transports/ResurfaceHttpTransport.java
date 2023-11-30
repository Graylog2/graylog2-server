package org.graylog2.inputs.transports;

import com.google.common.eventbus.EventBus;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import okhttp3.OkHttpClient;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.BooleanField;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.DropdownField;
import org.graylog2.plugin.configuration.fields.ListField;
import org.graylog2.plugin.configuration.fields.NumberField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import org.graylog2.plugin.inputs.transports.ThrottleableTransport;
import org.graylog2.plugin.inputs.transports.Transport;
import org.graylog2.security.encryption.EncryptedValueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.lang.System.currentTimeMillis;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static org.graylog2.plugin.Message.FIELD_ID;
import static org.graylog2.plugin.Message.FIELD_TIMESTAMP;

public class ResurfaceHttpTransport extends HttpPollTransport {

    private static final Logger LOG = LoggerFactory.getLogger(ResurfaceHttpTransport.class);

    private static final String CK_URL = "target_url";
    private static final String CK_HTTP_METHOD = "http_method";
    private static final String CK_HTTP_BODY = "http_body";
    private static final String CK_CONTENT_TYPE = "content_type";
    private static final String CK_HEADERS = "headers";
    private static final String CK_TIMEUNIT = "timeunit";
    private static final String CK_INTERVAL = "interval";

    private static final String CK_HOSTNAME = "resurface_host";
    private static final String CK_PORT = "resurface_port";
    private static final String CK_COLUMNS = "resurface_columns";
    private static final String CK_USERNAME = "resurface_username";
    private static final String CK_PASSWORD = "resurface_password";
    private static final String CK_TLS_ENABLED = "tls_enabled";

    private static final String DB_PATH = "/ui/api/resurface/runsql";
    private static final String[] DB_COLUMNS = new String[] {"id","host",
            "custom_fields","custom_fields_array", "session_fields","session_fields_array",
            "agent_category","agent_device","agent_name",
            "graphql_operation_name","graphql_operation_type","graphql_query","graphql_variables",
            "graphql_batch","graphql_operations","graphql_operations_array","graphql_operations_count",
            "interval_category","interval_clique","interval_millis",
            "request_address","request_body","request_content_type","request_headers","request_headers_array",
            "request_json_array","request_json_object","request_json_type",
            "request_method","request_method_safe","request_params","request_params_array",
            "request_path","request_path_safe","request_port","request_protocol","request_query","request_url",
            "request_user_agent",
            "response_body","response_code","response_code_int","response_content_type",
            "response_date","response_day_of_month","response_day_of_week",
            "response_headers","response_headers_array","response_hour_of_day",
            "response_json_array","response_json_object","response_json_type","response_status",
            "response_time","response_time_millis","response_with_pii",
            "size_category","size_request_bytes","size_response_bytes","size_total_bytes"};
    private static final Map<String, String> DB_MAP_COLUMNS = Arrays.stream(DB_COLUMNS).collect(
//            Collectors.toMap(ResurfaceHttpTransport::toHuman, columnName -> columnName)
            Collectors.toMap(columnName -> columnName, columnName -> columnName)
    );
    private final Configuration configuration;
    private final EncryptedValueService encryptedValueService;

    @AssistedInject
    public ResurfaceHttpTransport(@Assisted Configuration configuration,
                                  EventBus serverEventBus,
                                  ServerStatus serverStatus,
                                  @Named("daemonScheduler") ScheduledExecutorService scheduler,
                                  OkHttpClient httpClient,
                                  EncryptedValueService encryptedValueService) {
        super(configuration, serverEventBus, serverStatus, scheduler, httpClient, encryptedValueService);

        this.configuration = configuration;
        this.encryptedValueService = encryptedValueService;

        this.configuration.setString(CK_HTTP_METHOD, POST);
        this.configuration.setString(CK_CONTENT_TYPE, TEXT_PLAIN);
        this.configuration.setString(CK_HEADERS, buildCredentials());
        this.configuration.setString(CK_URL, buildURL());
        this.configuration.setString(CK_HTTP_BODY, buildQuery());
    }

//    private static String toHuman(String snakeCasedName) {
//        if (snakeCasedName.equals("id")) return "ID";
//
//        String capitalized = (char) (snakeCasedName.charAt(0) & 0x5f) + snakeCasedName.substring(1);
//
//        int firstUnderscoreIndex = capitalized.indexOf('_');
//        if (firstUnderscoreIndex == -1) return capitalized;
//
//        String firstWord = capitalized.substring(0, firstUnderscoreIndex);
//        String rest = capitalized.substring(firstUnderscoreIndex).replace('_', ' ').trim();
//
//        return "%s: %s".formatted(firstWord, rest);
//    }

    private String buildQuery() {
        String commaJoined = String.join(",", configuration.getList(CK_COLUMNS));
        String commaJoinedWithMillis = "%s%s".formatted(commaJoined, configuration.getList(CK_COLUMNS)
                .contains("response_time_millis") ? "" : ",response_time_millis");
        long pollingMillis = currentTimeMillis() - TimeUnit.valueOf(configuration.getString(CK_TIMEUNIT))
                .toMillis(configuration.getInt(CK_INTERVAL));

        String query = ("SELECT id as %s,%s,%s FROM (" +
                "SELECT id,%s,substring(date_format(response_time, '%%Y-%%m-%%d %%H:%%i:%%S.%%f'),1,23) as %s " +
                "FROM resurface.data.messages " +
                "WHERE response_time_millis > %d " +
                "LIMIT 10" +
                ")").formatted(FIELD_ID, commaJoined, FIELD_TIMESTAMP,
                commaJoinedWithMillis, FIELD_TIMESTAMP, pollingMillis);
        LOG.debug("SQL query: %s".formatted(query));
        return query;
    }

    private String buildCredentials() {
        String encoded = Base64.getEncoder().encodeToString(
                "%s:%s".formatted(configuration.getString(CK_USERNAME),
                        Objects.requireNonNullElse(
                                encryptedValueService.decrypt(configuration.getEncryptedValue(CK_PASSWORD)),
                                ""
                        )
                ).getBytes(StandardCharsets.UTF_8)
        );
        LOG.debug("Encoded credentials: %s".formatted(encoded));
        return "Authorization: Basic %s".formatted(encoded);
    }

    private String buildURL() {
        String url = "http%s://%s:%d%s".formatted(
                configuration.getBoolean(CK_TLS_ENABLED) ? "s" : "",
                configuration.getString(CK_HOSTNAME), configuration.getInt(CK_PORT), DB_PATH
        );
        LOG.debug("Resurface DB query endpoint: %s".formatted(url));
        return url;
    }

    @FactoryClass
    public interface Factory extends Transport.Factory<ResurfaceHttpTransport> {
        @Override
        ResurfaceHttpTransport create(Configuration configuration);

        @Override
        Config getConfig();
    }

    @ConfigClass
    public static class Config extends ThrottleableTransport.Config {
        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            final ConfigurationRequest r = super.getRequestedConfiguration();

            r.addField(new TextField(
                    CK_HOSTNAME,
                    "Hostname",
                    "my-resurface-instance.com",
                    "Domain name used to connect to the Resurface host",
                    ConfigurationField.Optional.NOT_OPTIONAL,
                    1
            ));

            r.addField(new NumberField(
                    CK_PORT,
                    "Port",
                    80,
                    "Port number through which Resurface is exposed",
                    ConfigurationField.Optional.OPTIONAL,
                    3
            ));

            r.addField(new BooleanField(
                    CK_TLS_ENABLED,
                    "TLS",
                    false,
                    "Enable if Resurface DB is exposed through HTTPS",
                    2
            ));

//            r.addField(new TextField(
//                    CK_HTTP_BODY,
//                    "Query",
//                    "SELECT COUNT(*) AS Total FROM resurface.data.messages",
//                    "SQL Query to run against the Resurface data lake",
//                    ConfigurationField.Optional.OPTIONAL,
//            ));

            r.addField(new ListField(
                    CK_COLUMNS,
                    "Columns",
//                    Arrays.asList("all"),
                    Collections.emptyList(),
                    DB_MAP_COLUMNS,
                    "List of fields to be queried from Resurface data lake",
                    ConfigurationField.Optional.NOT_OPTIONAL,
                    6
            ));

            r.addField(new TextField(
                    CK_USERNAME,
                    "Resurface username",
                    "guest",
                    "Identifier for client authentication",
                    ConfigurationField.Optional.OPTIONAL,
                    4
            ));

            r.addField(new TextField(
                    CK_PASSWORD,
                    "Resurface password",
                    "",
                    "Secret password for client authentication",
                    ConfigurationField.Optional.OPTIONAL,
                    true,
                    5
            ));

            r.addField(new NumberField(
                    CK_INTERVAL,
                    "Interval",
                    1,
                    "Time between every collector run. Select a time unit in the corresponding dropdown. " +
                            "Example: Run every 5 minutes.",
                    ConfigurationField.Optional.NOT_OPTIONAL,
                    7
            ));

            r.addField(new DropdownField(
                    CK_TIMEUNIT,
                    "Interval time unit",
                    TimeUnit.MINUTES.toString(),
                    DropdownField.ValueTemplates.timeUnits(),
                    ConfigurationField.Optional.NOT_OPTIONAL
            ));

            return r;
        }
    }
}

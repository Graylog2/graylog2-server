package org.graylog.integrations.dbconnector.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import org.graylog.integrations.dbconnector.DBConnectorCodec;
import org.graylog.integrations.dbconnector.DBConnectorInput;
import org.graylog.integrations.dbconnector.DBConnectorUtils;
import org.graylog.integrations.dbconnector.api.requests.DBConnectorCreateInputRequest;
import org.graylog.integrations.dbconnector.api.requests.DBConnectorRequestImpl;
import org.graylog.integrations.dbconnector.external.DBConnectorClient;
import org.graylog.integrations.dbconnector.external.DBConnectorClientFactory;
import org.graylog.integrations.dbconnector.external.DBConnectorTransferObject;
import org.graylog2.database.NotFoundException;
import org.graylog2.inputs.Input;
import org.graylog2.inputs.InputService;
import org.graylog2.plugin.configuration.ConfigurationException;
import org.graylog2.plugin.database.users.User;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.transports.ThrottleableTransport2;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.rest.models.system.inputs.requests.InputCreateRequest;
import org.graylog2.shared.inputs.MessageInputFactory;
import org.graylog2.shared.inputs.NoSuchInputTypeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

import static org.graylog.integrations.dbconnector.DBConnectorProperty.MONGODB;

public class DBConnectorDriver {

    private final InputService inputService;
    private final MessageInputFactory messageInputFactory;
    private final NodeId nodeId;
    private final DBConnectorClientFactory DBConnectorClientFactory;

    @Inject
    public DBConnectorDriver(InputService inputService,
                             MessageInputFactory messageInputFactory,
                             NodeId nodeId,
                             DBConnectorClientFactory DBConnectorClientFactory) {

        this.inputService = inputService;
        this.messageInputFactory = messageInputFactory;
        this.nodeId = nodeId;
        this.DBConnectorClientFactory = DBConnectorClientFactory;
    }

    private static final Logger LOG = LoggerFactory.getLogger(DBConnectorDriver.class);

    public JsonNode checkCredentials(DBConnectorRequestImpl request) throws Exception {
        DBConnectorClient dbConnectorClient = DBConnectorClientFactory.getClient(request.dbType());
        String connectionString = DBConnectorUtils.buildConnectionString(request.dbType(), request.hostname(), request.port(),
                request.dbName(), request.username(), request.password());
        dbConnectorClient.getConnection(connectionString);
        DBConnectorTransferObject dto;
        if (request.dbType().equals(MONGODB)) {
            dto = DBConnectorTransferObject.builder()
                    .databaseType(request.dbType())
                    .databaseName(request.dbName())
                    .mongoCollectionName(request.mongoCollectionName()).build();
        } else {
            dto = DBConnectorTransferObject.builder().databaseType(request.dbType())
                    .tableName(request.tableName()).build();
        }
        return dbConnectorClient.validateConnection(dto);

    }

    public Input saveInput(DBConnectorCreateInputRequest request, User user) throws Exception {

        final HashMap<String, Object> configuration = new HashMap<>();
        configuration.put(DBConnectorCodec.NAME, request.name());
        configuration.put(ThrottleableTransport2.CK_THROTTLING_ALLOWED, request.throttlingAllowed());
        configuration.put(DBConnectorInput.CK_POLLING_INTERVAL, request.pollingInterval());
        configuration.put(DBConnectorInput.CK_POLLING_TIME_UNIT, request.pollingTimeUnit().name());
        configuration.put(DBConnectorInput.CK_HOSTNAME, request.hostname());
        configuration.put(DBConnectorInput.CK_PORT, request.port());
        configuration.put(DBConnectorInput.CK_DATABASE_NAME, request.dbName());
        configuration.put(DBConnectorInput.CK_USERNAME, request.username());
        configuration.put(DBConnectorInput.CK_PASSWORD, request.password());
        configuration.put(DBConnectorInput.CK_DATABASE_TYPE, request.dbType());
        configuration.put(DBConnectorInput.CK_TABLE_NAME, request.tableName());
        configuration.put(DBConnectorInput.CK_STATE_FILED, request.stateField());
        configuration.put(DBConnectorInput.CK_STATE_FIELD_TYPE, request.stateFieldType());
        configuration.put(DBConnectorInput.CK_MONGO_COLLECTION_NAME, request.mongoCollectionName());
        configuration.put(DBConnectorInput.CK_OVERRIDE_SOURCE, request.overrideSource());

        final InputCreateRequest inputCreateRequest = InputCreateRequest.create(request.name(),
                DBConnectorInput.TYPE,
                false,
                configuration,
                nodeId.toString());
        try {
            final MessageInput messageInput = messageInputFactory.create(inputCreateRequest, user.getName(), nodeId.toString());
            messageInput.checkConfiguration();
            final Input input = this.inputService.create(messageInput.asMap());
            final String newInputId = inputService.save(input);
            LOG.info("New Database Connector input created. id [{}] request [{}]", newInputId, request);

            return input;
        } catch (NoSuchInputTypeException e) {
            LOG.error("There is no such input type registered.", e);
            throw new NotFoundException("There is no such input type registered.", e);
        } catch (ConfigurationException e) {
            LOG.error("Missing or invalid input configuration.", e);
            throw new BadRequestException("Missing or invalid input configuration.", e);
        }
    }
}

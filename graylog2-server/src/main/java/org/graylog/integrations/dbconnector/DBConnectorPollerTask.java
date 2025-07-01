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
package org.graylog.integrations.dbconnector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.integrations.dbconnector.external.DBConnectorClient;
import org.graylog.integrations.dbconnector.external.DBConnectorTransferObject;
import org.graylog.integrations.dbconnector.external.model.DBConnectorEndpoints;
import org.graylog2.inputs.persistence.InputStatusRecord;
import org.graylog2.inputs.persistence.InputStatusService;
import org.graylog2.plugin.InputFailureRecorder;
import org.graylog2.plugin.journal.RawMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.graylog.integrations.dbconnector.DBConnectorInput.CK_DATABASE_NAME;
import static org.graylog.integrations.dbconnector.DBConnectorInput.CK_DATABASE_TYPE;
import static org.graylog.integrations.dbconnector.DBConnectorInput.CK_HOSTNAME;
import static org.graylog.integrations.dbconnector.DBConnectorInput.CK_MONGO_COLLECTION_NAME;
import static org.graylog.integrations.dbconnector.DBConnectorInput.CK_PASSWORD;
import static org.graylog.integrations.dbconnector.DBConnectorInput.CK_PORT;
import static org.graylog.integrations.dbconnector.DBConnectorInput.CK_STATE_FIELD_TYPE;
import static org.graylog.integrations.dbconnector.DBConnectorInput.CK_STATE_FILED;
import static org.graylog.integrations.dbconnector.DBConnectorInput.CK_TABLE_NAME;
import static org.graylog.integrations.dbconnector.DBConnectorInput.CK_TIMEZONE;
import static org.graylog.integrations.dbconnector.DBConnectorInput.CK_USERNAME;
import static org.graylog.integrations.dbconnector.DBConnectorProperty.DB_CONNECTOR_OFFSET;
import static org.graylog.integrations.dbconnector.DBConnectorProperty.MONGODB;
import static org.graylog.integrations.dbconnector.DBConnectorProperty.TIMESTAMP;

public class DBConnectorPollerTask implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(DBConnectorPollerTask.class);

    private final DBConnectorInput input;
    private final InputStatusService inputStatusService;
    private final DBConnectorClient dbConnectorClient;
    private final DBConnectorTransport transport;
    private final DBConnectorTransferObject.Builder dto;
    private final InputFailureRecorder inputFailureRecorder;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public DBConnectorPollerTask(DBConnectorInput input, InputStatusService inputStatusService,
                                 DBConnectorClient DBConnectorClient, DBConnectorTransport DBConnectorTransport,
                                 DBConnectorTransferObject.Builder transferObject, InputFailureRecorder inputFailureRecorder) {
        this.input = input;
        this.inputStatusService = inputStatusService;
        this.dbConnectorClient = DBConnectorClient;
        this.transport = DBConnectorTransport;
        this.dto = transferObject;
        this.inputFailureRecorder = inputFailureRecorder;
    }

    @Override
    public void run() {
        try {
            doRun();
            inputFailureRecorder.setRunning();
        } catch (Throwable t) {
            inputFailureRecorder.setFailing(getClass(), "Failure in Poller Task", t);
        }
    }

    public void doRun() throws Exception {

        if (transport.isThrottled()) {
            LOG.info("[throttled] The Database input will pause message processing until the throttle state clears.");
            transport.blockUntilUnthrottled();
            LOG.info("[unthrottled] The Database input will now resume processing records.");
        }

        final String dbType = input.getConfiguration().getString(CK_DATABASE_TYPE);
        final String hostname = input.getConfiguration().getString(CK_HOSTNAME);
        final int port = input.getConfiguration().getInt(CK_PORT);
        final String databaseName = input.getConfiguration().getString(CK_DATABASE_NAME);
        final String username = input.getConfiguration().getString(CK_USERNAME);
        final String password = input.getConfiguration().getString(CK_PASSWORD);
        final String timezone = input.getConfiguration().getString(CK_TIMEZONE, "UTC");

        LOG.debug("Input [{}] :: Executing Database PollerTask", input.getId());
        String connectionString = DBConnectorUtils.buildConnectionString(
                DBConnectorEndpoints.getEnum(dbType), hostname, port, databaseName, username, password);
        dbConnectorClient.getConnection(connectionString);
        String stateFieldType = input.getConfiguration().getString(CK_STATE_FIELD_TYPE);
        String stateField = input.getConfiguration().getString(CK_STATE_FILED);
        Map<String, String> checkpointMap = new HashMap<>();
        try {
            checkpointMap = fetchInputCheckpoint(input.getId());
        } catch (Exception e) {
            LOG.debug("No checkpoint exists [{}]", e.toString());
        }

        int processedRecordCount = 0;

        ZoneId zoneId = ZoneId.of(timezone);
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        Timestamp defaultStartTime = Timestamp.from(now.minusHours(1).toInstant());
        Timestamp endTime = Timestamp.from(now.toInstant());
        Timestamp startTime = null;

        long offset = 0;
        List<String> fetchedRecords;

        if (stateFieldType.equals(TIMESTAMP)) {
            startTime = checkpointMap.containsKey(DB_CONNECTOR_OFFSET)
                    ? parseCheckpointTimestamp(checkpointMap.get(DB_CONNECTOR_OFFSET))
                    : defaultStartTime;
        } else {
            offset = checkpointMap.containsKey(DB_CONNECTOR_OFFSET)
                    ? Long.parseLong(checkpointMap.get(DB_CONNECTOR_OFFSET))
                    : 0;
        }

        setConfigFields();

        dto.stateFieldValue((startTime != null)
                ? startTime
                : offset);

        fetchedRecords = dbConnectorClient.fetchLogs(dto.build());
        ObjectMapper mapper = new ObjectMapper();

        for (String record : fetchedRecords) {
            JsonNode node = mapper.readTree(record);
            if (node.isArray()) {
                for (JsonNode element : node) {
                    String json = element.toString();
                    input.processRawMessage(new RawMessage(json.getBytes(StandardCharsets.UTF_8)));
                    processedRecordCount++;
                }
            } else {
                input.processRawMessage(new RawMessage(record.getBytes(StandardCharsets.UTF_8)));
                processedRecordCount++;
            }
        }

        if (stateFieldType.equals(TIMESTAMP)) {
            endTime = !fetchedRecords.isEmpty() ? updateTimestamp(fetchedRecords, stateField) : endTime;
            checkpointMap.put(DB_CONNECTOR_OFFSET, endTime.toString());
        } else {
            offset = !fetchedRecords.isEmpty() ? updateOffset(fetchedRecords, stateField) : 0;
            checkpointMap.put(DB_CONNECTOR_OFFSET, String.valueOf(offset));
        }

        if (processedRecordCount > 0) {
            storeInputCheckpoint(checkpointMap);
        }

        LOG.debug("Input [{}] :: Processed [{}] Database events", input.getId(), processedRecordCount);
        LOG.debug("Input [{}] :: Storing Database checkpoints [{}]", input.getId(), checkpointMap);
    }

    private void storeInputCheckpoint(Map<String, String> checkpointMap) {

        DBConnectorCheckpoint dbConnectorCheckpoint = DBConnectorCheckpoint.builder()
                .lastEventTime(checkpointMap.get(DB_CONNECTOR_OFFSET))
                .build();
        inputStatusService.save(InputStatusRecord.builder()
                .inputId(input.getId())
                .inputStateData(dbConnectorCheckpoint)
                .build());
    }

    private Map<String, String> fetchInputCheckpoint(String inputId) {

        DBConnectorCheckpoint dbConnectorCheckpoint;
        Optional<InputStatusRecord> optionalStatusRecord = inputStatusService.get(inputId);
        Map<String, String> checkpoints = new HashMap<>();
        if (optionalStatusRecord.isPresent()) {
            dbConnectorCheckpoint = (DBConnectorCheckpoint) optionalStatusRecord.get().inputStateData();
            LOG.debug("Fetching stored DBConnector checkpoints[{}] for Input [{}]", dbConnectorCheckpoint, input.getId());
            checkpoints.put(DB_CONNECTOR_OFFSET, dbConnectorCheckpoint.lastEventTime());
        }
        return checkpoints;
    }

    private Timestamp parseCheckpointTimestamp(String raw) {
        try {
            return Timestamp.from(Instant.parse(raw));
        } catch (Exception e1) {
            try {
                return Timestamp.valueOf(raw);
            } catch (Exception e2) {
                throw new IllegalStateException("Unable to parse checkpoint timestamp: " + raw, e2);
            }
        }
    }

    private Timestamp updateTimestamp(List<String> fetchedRecords, String stateField) throws IOException {
        String lastRecord = fetchedRecords.get(fetchedRecords.size() - 1);
        JsonNode root = objectMapper.readTree(lastRecord);

        JsonNode targetNode;
        if (root.isArray()) {
            targetNode = root.get(root.size() - 1);
        } else {
            targetNode = root;
        }

        JsonNode timestampNode = targetNode.get(stateField);
        // Handle MongoDB extended JSON format: { "$date": "..." }
        if (timestampNode != null && timestampNode.isObject() && timestampNode.has("$date")) {
            timestampNode = timestampNode.get("$date");
        }
        if (timestampNode == null || timestampNode.isNull()) {
            throw new IllegalStateException("Timestamp field '" + stateField + "' not found or is null in record: " + targetNode.toPrettyString());
        }
        String rawTimestamp = timestampNode.asText();
        try {
            Instant instant = Instant.parse(rawTimestamp);
            return Timestamp.from(instant);
        } catch (Exception e1) {
            try {
                return Timestamp.valueOf(rawTimestamp.replace("T", " "));
            } catch (Exception e2) {
                throw new IllegalStateException("Unable to parse timestamp from field '" + stateField + "': " + rawTimestamp, e2);
            }
        }
    }

    private long updateOffset(List<String> fetchedRecords, String stateField) throws IOException {
        JsonNode root = objectMapper.readTree(fetchedRecords.get(fetchedRecords.size() - 1));
        JsonNode targetNode;
        // Handle both array and single object
        if (root.isArray()) {
            targetNode = root.get(root.size() - 1); // get last item in the array
        } else {
            targetNode = root;
        }

        JsonNode offsetNode = targetNode.get(stateField);
        if (offsetNode == null) {
            throw new IllegalStateException("State field '" + stateField + "' not found in the last record: " + targetNode.toPrettyString());
        }

        long offset;
        if (offsetNode.isNumber()) {
            offset = offsetNode.asLong();
        } else if (offsetNode.isTextual()) {
            try {
                offset = Long.parseLong(offsetNode.asText());
            } catch (NumberFormatException e) {
                throw new IllegalStateException("State field '" + stateField + "' is not a valid number: " + offsetNode.toPrettyString(), e);
            }
        } else {
            throw new IllegalStateException("State field '" + stateField + "' is not numeric or stringified number: " + offsetNode.toPrettyString());
        }

        return offset;
    }


    private void setConfigFields() {
        if (input.getConfiguration().getString(CK_DATABASE_TYPE).equalsIgnoreCase(MONGODB)) {
            dto.databaseName(input.getConfiguration().getString(CK_DATABASE_NAME));
            dto.mongoCollectionName(input.getConfiguration().getString(CK_MONGO_COLLECTION_NAME));

        } else dto.tableName(input.getConfiguration().getString(CK_TABLE_NAME));
        dto.stateFieldType(input.getConfiguration().getString(CK_STATE_FIELD_TYPE));
        dto.stateField(input.getConfiguration().getString(CK_STATE_FILED));
    }
}

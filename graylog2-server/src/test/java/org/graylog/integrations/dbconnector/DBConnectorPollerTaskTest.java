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

import org.graylog.integrations.dbconnector.external.DBConnectorClient;
import org.graylog.integrations.dbconnector.external.DBConnectorTransferObject;
import org.graylog2.inputs.persistence.InputStatusRecord;
import org.graylog2.inputs.persistence.InputStatusService;
import org.graylog2.plugin.InputFailureRecorder;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.journal.RawMessage;
import org.joda.time.DateTimeUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.graylog.integrations.dbconnector.DBConnectorInput.CK_DATABASE_NAME;
import static org.graylog.integrations.dbconnector.DBConnectorInput.CK_DATABASE_TYPE;
import static org.graylog.integrations.dbconnector.DBConnectorInput.CK_MONGO_COLLECTION_NAME;
import static org.graylog.integrations.dbconnector.DBConnectorInput.CK_STATE_FIELD_TYPE;
import static org.graylog.integrations.dbconnector.DBConnectorInput.CK_STATE_FILED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class DBConnectorPollerTaskTest {
    // Code Under Test
    @InjectMocks
    private DBConnectorPollerTask cut;

    // Mock Objects
    @Mock
    private DBConnectorInput mockInput;
    @Mock
    private InputStatusService mockInputStatusService;
    @Mock
    private DBConnectorClient mockClient;
    @Mock
    private DBConnectorTransport mockTransport;
    @Mock
    DBConnectorTransferObject.Builder transferObject;
    @Mock
    InputFailureRecorder mockInputFailurerecorder;
    @Mock
    Configuration mockConfiguration;

    private static final String TEST_INPUT_ID = "Test Input ID";

    @Before
    public void setUp() {
        cut = new DBConnectorPollerTask(mockInput, mockInputStatusService, mockClient, mockTransport, transferObject, mockInputFailurerecorder);
    }

    @AfterClass
    public static void releaseFixedDate() {
        DateTimeUtils.setCurrentMillisSystem();
    }

    // Test Cases
    @Test
    public void run_waitsForThrottleRelease_whenTransportIsThrottled() {
        givenTransportIsThrottled();
        whenRunIsCalled();
        thenBlockUntilThrottleWillBeCalled();
    }

    @Test
    public void run_doesNotWaitForThrottleRelease_whenTransportIsNotThrottled() {
        givenTransportIsNotThrottled();
        whenRunIsCalled();
        thenBlockUntilThrottleWillNotBeCalled();
    }

    @Test
    public void run_queriesForMultipleLogTypes_whenMultipleLogTypesConfigured() throws Exception {
        givenTransportIsNotThrottled();
        givenGoodInputId();
        givenContentIsAvailableForIncrementType("increment");
        whenRunIsCalled();
        thenBlockUntilThrottleWillNotBeCalled();
        thenRecordsPersistedToJournal(5);
        thenClientQueriedForLogTypes(1);
        thenCheckpointPersisted(1);
        thenInputIsNotShutDown();
    }

    @Test
    public void run_usesGoodStartAndEndTimes_whenMongoCollection() throws Exception {
        givenTransportIsNotThrottled();
        givenGoodInputId();
        givenCheckpoint(Date.from(Instant.parse("2020-05-29T13:29:24.527Z")));
        givenMongoDatabase();

        whenRunIsCalled();

        thenBlockUntilThrottleWillNotBeCalled();
        thenMongoLogsQueried(1);
        thenCheckpointPersisted(1);
        thenInputIsNotShutDown();
    }


    @Test
    public void run_fetchesAndPersistsLogs_whenSQLConnection() throws Exception {
        givenTransportIsNotThrottled();
        givenGoodInputId();
        givenContentIsAvailable("Timestamp");
        givenCheckpointTimestamp(Timestamp.valueOf("2020-05-29 13:29:24.527"));

        whenRunIsCalled();

        thenBlockUntilThrottleWillNotBeCalled();
        thenClientQueriedForLogTypes(1);
        thenContentQueryIsExecuted();
        thenRecordsPersistedToJournal(5);
        thenCheckpointPersisted(1);
        thenInputIsNotShutDown();
    }

    // GIVENs
    private void givenGoodInputId() {
        given(mockInput.getId()).willReturn(TEST_INPUT_ID);
        given(mockConfiguration.getString(DBConnectorInput.CK_HOSTNAME)).willReturn("localhost");
        given(mockConfiguration.getString(CK_DATABASE_NAME)).willReturn("test_db");
        given(mockConfiguration.getString(DBConnectorInput.CK_TABLE_NAME)).willReturn("test_table");
        given(mockConfiguration.getString(DBConnectorInput.CK_MONGO_COLLECTION_NAME)).willReturn("test_collection");
        given(mockConfiguration.getString(DBConnectorInput.CK_TIMEZONE, "UTC")).willReturn("UTC");
        given(mockConfiguration.getString(CK_DATABASE_TYPE)).willReturn("Oracle");
        given(mockConfiguration.getString(CK_STATE_FILED)).willReturn("records");
        given(mockInput.getConfiguration()).willReturn(mockConfiguration);

    }

    private void givenTransportIsThrottled() {
        given(mockTransport.isThrottled()).willReturn(true);
    }

    private void givenTransportIsNotThrottled() {
        given(mockTransport.isThrottled()).willReturn(false);
    }

    private void thenRecordsPersistedToJournal(int recordCount) {
        verify(mockInput, times(recordCount)).processRawMessage(any(RawMessage.class));
    }

    private void givenCheckpoint(Date checkpoint) {
        given(mockInputStatusService.get(TEST_INPUT_ID)).willReturn(
                Optional.of(InputStatusRecord.builder()
                        .inputId(TEST_INPUT_ID)
                        .inputStateData(
                                DBConnectorCheckpoint.builder()
                                        .lastEventTime(checkpoint.toInstant().toString())
                                        .build())
                        .build()));
    }

    private void givenCheckpointTimestamp(Timestamp checkpoint) {
        given(mockInputStatusService.get(TEST_INPUT_ID)).willReturn(
                Optional.of(InputStatusRecord.builder()
                        .inputId(TEST_INPUT_ID)
                        .inputStateData(
                                DBConnectorCheckpoint.builder()
                                        .lastEventTime(checkpoint.toString())
                                        .build())
                        .build()));
    }

    private void givenContentIsAvailable(String stateFieldType) throws Exception {
        given(mockConfiguration.getString(CK_STATE_FIELD_TYPE)).willReturn(stateFieldType);
        given(mockConfiguration.getString(CK_STATE_FILED)).willReturn("created_datetime");
        List<String> records = List.of(
                "{\"records\": 2, \"created_datetime\": \"2020-05-29 13:29:24.527\"}",
                "{\"records\": 3, \"created_datetime\": \"2020-05-30 09:15:10.123\"}",
                "{\"records\": 4, \"created_datetime\": \"2020-05-31 17:42:01.456\"}",
                "{\"records\": 5, \"created_datetime\": \"2020-06-01 08:00:00.000\"}",
                "{\"records\": 6, \"created_datetime\": \"2020-06-02 19:45:30.789\"}"
        );
        given(mockClient.fetchLogs(any())).willReturn(records);

    }

    private void givenContentIsAvailableForIncrementType(String stateFieldType) throws Exception {
        given(mockConfiguration.getString(CK_STATE_FIELD_TYPE)).willReturn(stateFieldType);
        List<String> records = List.of(
                "{\"records\": 2, \"created_datetime\": \"2020-05-29 13:29:24.527\"}",
                "{\"records\": 3, \"created_datetime\": \"2020-05-30 09:15:10.123\"}",
                "{\"records\": 4, \"created_datetime\": \"2020-05-31 17:42:01.456\"}",
                "{\"records\": 5, \"created_datetime\": \"2020-06-01 08:00:00.000\"}",
                "{\"records\": 6, \"created_datetime\": \"2020-06-02 19:45:30.789\"}"
        );
        given(mockClient.fetchLogs(any())).willReturn(records);

    }

    private void givenMongoDatabase() throws Exception {
        given(mockConfiguration.getString(CK_STATE_FIELD_TYPE)).willReturn("Timestamp");
        given(mockConfiguration.getString(CK_DATABASE_TYPE)).willReturn("MongoDB");
        given(mockConfiguration.getString(CK_MONGO_COLLECTION_NAME)).willReturn("collection_name");
        given(mockConfiguration.getString(CK_DATABASE_NAME)).willReturn("database_name");
        given(mockConfiguration.getString(CK_STATE_FILED)).willReturn("created_datetime");
        given(mockInput.getConfiguration()).willReturn(mockConfiguration);
        List<String> records = List.of(
                "{\"records\": 2, \"created_datetime\": \"2020-05-29T13:29:24.527Z\"}",
                "{\"records\": 3, \"created_datetime\": \"2020-05-30T09:15:10.123Z\"}",
                "{\"records\": 4, \"created_datetime\": \"2020-05-31T17:42:01.456Z\"}",
                "{\"records\": 5, \"created_datetime\": \"2020-06-01T08:00:00.000Z\"}",
                "{\"records\": 6, \"created_datetime\": \"2020-06-02T19:45:30.789Z\"}"
        );
        given(mockClient.fetchLogs(any())).willReturn(records);

    }

    // WHENS
    private void whenRunIsCalled() {
        cut.run();
    }

    // THENs
    private void thenBlockUntilThrottleWillBeCalled() {
        verify(mockTransport, times(1)).blockUntilUnthrottled();
    }

    private void thenBlockUntilThrottleWillNotBeCalled() {
        verify(mockTransport, times(0)).blockUntilUnthrottled();
    }

    private void thenClientQueriedForLogTypes(int count) throws SQLException {
        verify(mockClient, times(count)).fetchLogs(any());
    }

    private void thenMongoLogsQueried(int count) throws SQLException {
        verify(mockClient, times(count)).fetchLogs(any());
    }

    private void thenContentQueryIsExecuted() throws SQLException {
        verify(mockClient, times(1)).fetchLogs(any());
    }

    private void thenCheckpointPersisted(int count) {
        verify(mockInputStatusService, times(count)).save(any(InputStatusRecord.class));
    }

    private void thenInputIsNotShutDown() {
        verify(mockInput, times(0)).fail(any(Throwable.class));
    }
}

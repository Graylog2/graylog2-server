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
package org.graylog.integrations.dbconnector.external;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SQLClientTest {

    @InjectMocks
    private SQLClient client;
    @Mock
    private Connection mockConnection;
    @Mock
    private PreparedStatement mockStatement;
    @Mock
    private ResultSet mockResultSet;
    @Mock
    private ResultSetMetaData mockMetaData;
    @Mock
    private DBConnectorTransferObject dto;

    private void givenValidDTO() {
        when(dto.tableName()).thenReturn("TEST_TABLE");
        when(dto.stateFieldType()).thenReturn("Timestamp");
        when(dto.stateField()).thenReturn("created_at");
    }

    @Test
    public void validateConnection_shouldReturnJsonWithRows() throws Exception {
        // GIVEN
        givenValidDTO();

        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        when(mockStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.getMetaData()).thenReturn(mockMetaData);
        when(mockMetaData.getColumnCount()).thenReturn(1);
        when(mockMetaData.getColumnName(1)).thenReturn("id");
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getString(1)).thenReturn("123");

        // WHEN
        JsonNode result = client.validateConnection(dto);

        // THEN
        assertTrue(result.has("2")); // Starts from 1, increments before putting
        assertEquals("123", result.get("2").get("id").asText());
    }

    @Test
    public void getColumns_shouldReturnFormattedColumnString() throws Exception {
        givenValidDTO();
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        when(mockStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true, true, false);
        when(mockResultSet.getString(1)).thenReturn("ID", "NAME");
        client = Mockito.spy(client);
        String columns = client.getColumns("users");
        assertEquals("JSON_OBJECT('ID',ID,'NAME',NAME)", columns);
    }

    @Test
    public void patternCheck_shouldReturnInput_whenNoSpecialChars() {
        String input = "users";
        String result = client.patternCheck(input);
        assertEquals("users", result);
    }

    @Test
    public void patternCheck_shouldReturnQuotedInput_whenSpecialCharsPresent() {
        String input = "user$info";
        String result = client.patternCheck(input);
        assertEquals("`user$info`", result);
    }

    @Test
    public void addQueryCondition_shouldReturnTimestampCondition_whenTypeIsTimestamp() {
        givenValidDTO();
        client = Mockito.spy(client);
        client.setDTO(dto);
        String result = client.addQueryCondition("created_at");
        assertTrue(result.contains(" where `created_at` > TIMESTAMP( ? )"));
    }

    @Test
    public void addQueryCondition_shouldReturnIntegerCondition_whenTypeIsInteger() {
        when(dto.stateFieldType()).thenReturn("increment");

        client = Mockito.spy(client);
        client.setDTO(dto);
        String result = client.addQueryCondition("created_at");

        assertTrue(result.contains("where `created_at` > "));
    }

    @Test
    public void prepareQuery_shouldConstructCorrectSQL() throws Exception {
        // GIVEN
        givenValidDTO();
        when(dto.tableName()).thenReturn("users");
        when(dto.stateField()).thenReturn("id");

        client = Mockito.spy(new SQLClient());
        client.setDTO(dto);

        doReturn("JSON_OBJECT('id',id,'name',name)").when(client).getColumns("users");
        doReturn("users").when(client).patternCheck("users");
        doReturn(" WHERE id > 10").when(client).addQueryCondition("id");

        // WHEN
        String result = client.prepareQuery();

        // THEN
        String expected = "select JSON_OBJECT('id',id,'name',name) FROM users WHERE id > 10 ORDER BY id LIMIT ? OFFSET  ?;";
        assertEquals(expected, result);
    }
}


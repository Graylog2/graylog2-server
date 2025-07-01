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
public class DB2ClientTest {

    @InjectMocks
    private DB2Client client;
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
        when(mockResultSet.getString(1)).thenReturn("101");

        // WHEN
        JsonNode result = client.validateConnection(dto);

        // THEN
        assertTrue(result.has("1"));
        assertEquals("101", result.get("1").get("id").asText());
    }

    @Test
    public void getColumns_shouldReturnCommaSeparatedColumnNames() throws Exception {
        // GIVEN
        givenValidDTO();
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        when(mockStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true, true, false);
        when(mockResultSet.getString(1)).thenReturn("ID", "NAME");

        // WHEN
        String columns = client.getColumns("users");

        // THEN
        assertEquals("ID, NAME", columns);
    }

    @Test
    public void patternCheck_shouldReturnInput_whenNoSpecialChars() {
        givenValidDTO();
        String result = client.patternCheck("tableName");
        assertEquals("tableName", result);
    }

    @Test
    public void patternCheck_shouldReturnInput_whenSpecialChars() {
        givenValidDTO();
        String result = client.patternCheck("table$name!");
        assertEquals("\"table$name!\"", result);
    }

    @Test
    public void addQueryCondition_shouldReturnTimestampCondition_whenTypeIsTimestamp() {
        givenValidDTO();
        String result = client.addQueryCondition("created_at");
        String withQuotes = "\"created_at\"";
        assertTrue(result.contains(" where " + withQuotes + " > ( ? )"));
    }

    @Test
    public void addQueryCondition_shouldReturnIntegerCondition_whenTypeIsInteger() {
        givenValidDTO();
        when(dto.stateFieldType()).thenReturn("increment");
        String result = client.addQueryCondition("id");
        assertTrue(result.contains("where id > "));
    }

    @Test
    public void prepareQuery_shouldConstructSQLQuery() throws Exception {
        givenValidDTO();
        when(dto.tableName()).thenReturn("users");
        when(dto.stateField()).thenReturn("id");

        client = Mockito.spy(new DB2Client());
        client.setDTO(dto);

        doReturn("id, name, status").when(client).getColumns("users");
        doReturn("users").when(client).patternCheck("users");
        doReturn(" WHERE id > 10").when(client).addQueryCondition("id");

        String expectedQuery = "select id, name, status FROM users WHERE id > 10 ORDER BY id LIMIT ? OFFSET  ?";
        String actualQuery = client.prepareQuery();

        assertEquals(expectedQuery, actualQuery);
    }
}

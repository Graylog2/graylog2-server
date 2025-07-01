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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.graylog.integrations.dbconnector.DBConnectorProperty.BATCH_SIZE;
import static org.graylog.integrations.dbconnector.DBConnectorProperty.COUNT_QUERY;
import static org.graylog.integrations.dbconnector.DBConnectorProperty.DB2_COLUMN_QUERY;
import static org.graylog.integrations.dbconnector.DBConnectorProperty.FROM;
import static org.graylog.integrations.dbconnector.DBConnectorProperty.LIMIT_CLAUSE;
import static org.graylog.integrations.dbconnector.DBConnectorProperty.LIMIT_RECORDS;
import static org.graylog.integrations.dbconnector.DBConnectorProperty.OFFSET_CONDITION;
import static org.graylog.integrations.dbconnector.DBConnectorProperty.ORDER_BY;
import static org.graylog.integrations.dbconnector.DBConnectorProperty.RECORD_COUNT;
import static org.graylog.integrations.dbconnector.DBConnectorProperty.TEST_QUERY;
import static org.graylog.integrations.dbconnector.DBConnectorProperty.TIMESTAMP;
import static org.graylog.integrations.dbconnector.DBConnectorProperty.WHERE_CLAUSE;

public class DB2Client implements DBConnectorClient {

    private static final Logger LOG = LoggerFactory.getLogger(DB2Client.class);
    private Connection connection;
    ObjectMapper objectMapper = new ObjectMapper();
    private String collectQuery;
    private DBConnectorTransferObject dto;

    public void getConnection(String connectionString) throws SQLException {
        connection = DriverManager.getConnection(connectionString);
    }

    public JsonNode validateConnection(DBConnectorTransferObject transferObject) throws SQLException {
        Map<Object, Object> collection = new HashMap<>();
        setDTO(transferObject);
        PreparedStatement stmt = connection.prepareStatement(TEST_QUERY + patternCheck(dto.tableName()));
        try {
            stmt.setMaxRows(LIMIT_RECORDS);
            ResultSet rs = stmt.executeQuery();
            ResultSetMetaData result = rs.getMetaData();
            int columnsNumber = result.getColumnCount();
            int count = 0;
            while (rs.next()) {
                Map<Object, Object> map = new HashMap<>();
                for (int i = 1; i <= columnsNumber; i++) {
                    map.put(result.getColumnName(i), rs.getString(i));
                }
                count++;
                collection.put(count, map);
            }
        } catch (SQLException exception) {
            LOG.error("DB2 Server Connection error occurred ", exception);
            throw new SQLException(exception);
        } finally {
            stmt.closeOnCompletion();
            connection.close();
        }
        return objectMapper.valueToTree(collection);
    }

    public List<String> fetchLogs(DBConnectorTransferObject transferObject) throws SQLException {
        setDTO(transferObject);
        List<String> listLogs = new ArrayList<>();
        int offset = 0;

        if (collectQuery == null) {
            collectQuery = prepareQuery();
        }

        try {
            while (true) {
                try (PreparedStatement stmt = getPreparedStatement(collectQuery)) {
                    stmt.setInt(2, BATCH_SIZE);
                    stmt.setInt(3, offset);

                    try (ResultSet rs = stmt.executeQuery()) {
                        int columnCount = rs.getMetaData().getColumnCount();
                        int rowsFetched = 0;

                        while (rs.next()) {
                            ObjectNode row = objectMapper.createObjectNode();
                            ResultSetMetaData metaData = rs.getMetaData();
                            for (int i = 1; i <= columnCount; i++) {
                                String columnName = metaData.getColumnLabel(i);
                                row.put(columnName, rs.getString(i));
                            }
                            listLogs.add(objectMapper.writeValueAsString(row));
                            rowsFetched++;
                        }

                        if (rowsFetched < BATCH_SIZE) {
                            break; // Last batch
                        }

                        offset += BATCH_SIZE;
                    }
                }
            }
        } catch (SQLException exception) {
            LOG.error("DB2 logs fetch error: ", exception);
            throw exception;
        } catch (JsonProcessingException e) {
            LOG.error("Unable to parse JSON: ", e);
            throw new RuntimeException(e);
        } finally {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        }

        return listLogs;
    }

    public String getColumns(String tableName) throws SQLException {

        PreparedStatement stmt = connection.prepareStatement(DB2_COLUMN_QUERY);
        stmt.setString(1, tableName.toUpperCase());
        ResultSet rs;
        StringBuilder columns = new StringBuilder();
        try {
            rs = stmt.executeQuery();
            columns = new StringBuilder();
            String col;
            while (rs.next()) {
                col = rs.getString(1);
                columns.append(col).append(", ");
            }
            // Remove trailing comma and space if any columns were appended
            if (columns.length() >= 2) {
                columns.setLength(columns.length() - 2);
            }
        } catch (SQLException e) {
            LOG.error("DB2 Connection error ", e);
        }
        return columns.toString();
    }

    public int getRowCount() throws SQLException {
        String query = COUNT_QUERY + patternCheck(dto.tableName()) + addQueryCondition(dto.stateField());
        PreparedStatement stmt = getPreparedStatement(query);
        ResultSet result = stmt.executeQuery();
        result.next();
        return result.getInt(RECORD_COUNT);
    }

    public String patternCheck(String param) {
        // Validate the parameter and enclose it with quotes if contains special characters or space
        Pattern valid = Pattern.compile("[^a-z0-9]", Pattern.CASE_INSENSITIVE);
        Matcher matcher = valid.matcher(param);
        if (matcher.find()) {
            LOG.debug("Parameter contains special characters or spaces: {}", param);
            return "\"" + param + "\"";
        } else return param;
    }

    public PreparedStatement getPreparedStatement(String query) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        if (dto.stateFieldType().equals(TIMESTAMP)) stmt.setTimestamp(1, (Timestamp) dto.stateFieldValue());
        else stmt.setInt(1, Integer.parseInt(dto.stateFieldValue().toString()));
        return stmt;
    }

    public String addQueryCondition(String stateField) {
        if (dto.stateFieldType().equals(TIMESTAMP)) {
            return WHERE_CLAUSE + patternCheck(stateField) + " > ( ? )";
        } else {
            return WHERE_CLAUSE + patternCheck(stateField) + OFFSET_CONDITION;
        }
    }

    public String prepareQuery() throws SQLException {
        String columns = getColumns(dto.tableName());
        StringBuilder query = new StringBuilder();
        query.append("select ").append(columns).append(FROM).append(patternCheck(dto.tableName())).append(addQueryCondition(dto.stateField())).append(ORDER_BY).append(dto.stateField()).append(LIMIT_CLAUSE);
        return query.toString();
    }

    public void setDTO(DBConnectorTransferObject transferObject) {
        dto = transferObject;
    }
}

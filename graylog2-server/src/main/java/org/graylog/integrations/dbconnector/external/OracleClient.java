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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.graylog.integrations.dbconnector.DBConnectorProperty.BATCH_SIZE;
import static org.graylog.integrations.dbconnector.DBConnectorProperty.COUNT_QUERY;
import static org.graylog.integrations.dbconnector.DBConnectorProperty.LIMIT_RECORDS;
import static org.graylog.integrations.dbconnector.DBConnectorProperty.OFFSET_CONDITION;
import static org.graylog.integrations.dbconnector.DBConnectorProperty.ORDER_BY;
import static org.graylog.integrations.dbconnector.DBConnectorProperty.RECORD_COUNT;
import static org.graylog.integrations.dbconnector.DBConnectorProperty.SELECT_QUERY;
import static org.graylog.integrations.dbconnector.DBConnectorProperty.TEST_QUERY;
import static org.graylog.integrations.dbconnector.DBConnectorProperty.TIMESTAMP;
import static org.graylog.integrations.dbconnector.DBConnectorProperty.WHERE_CLAUSE;

public class OracleClient implements DBConnectorClient {

    private static final Logger LOG = LoggerFactory.getLogger(OracleClient.class);
    private Connection connection;
    private String collectQuery;
    private DBConnectorTransferObject dto;
    ObjectMapper objectMapper = new ObjectMapper();

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
            int count = 1;
            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                for (int i = 1; i <= columnsNumber; i++) {
                    map.put(result.getColumnName(i), rs.getString(i));
                }
                count++;
                collection.put(count, map);
            }
        } catch (SQLException exception) {
            LOG.error("Oracle SQL validation error: ", exception);
            throw exception;
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
                    stmt.setInt(2, offset);
                    stmt.setInt(3, BATCH_SIZE);

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
            LOG.error("Oracle fetch error: ", exception);
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


    public int getRowCount() throws SQLException {
        String query = COUNT_QUERY + patternCheck(dto.tableName()) + addQueryCondition(dto.stateField());
        PreparedStatement stmt = getPreparedStatement(query);
        ResultSet result = stmt.executeQuery();
        result.next();
        return result.getInt(RECORD_COUNT);
    }

    public String patternCheck(String param) {
        Pattern valid = Pattern.compile("[^a-z0-9]", Pattern.CASE_INSENSITIVE);
        Matcher matcher = valid.matcher(param);
        if (matcher.find()) {
            LOG.debug("Parameter contains special characters or spaces: {}", param);
            return "\"" + param.toUpperCase() + "\"";
        } else return param.toUpperCase();
    }

    public PreparedStatement getPreparedStatement(String query) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement(query, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        if (Objects.equals(dto.stateFieldType(), TIMESTAMP)) {
            stmt.setString(1, dto.stateFieldValue().toString());
        } else {
            stmt.setInt(1, Integer.parseInt(dto.stateFieldValue().toString()));
        }
        return stmt;
    }


    public String addQueryCondition(String stateField) {
        if (Objects.equals(dto.stateFieldType(), TIMESTAMP)) {
            return WHERE_CLAUSE + patternCheck(stateField) + " > TO_TIMESTAMP( ? , 'YYYY-MM-DD HH24:MI:SS.FF6')";
        } else {
            return WHERE_CLAUSE + patternCheck(stateField) + OFFSET_CONDITION;
        }
    }

    public String prepareQuery() {
        StringBuilder query = new StringBuilder();
        query.append(SELECT_QUERY)
                .append(patternCheck(dto.tableName()))
                .append(addQueryCondition(dto.stateField()))
                .append(ORDER_BY).append(dto.stateField())
                .append(" OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");
        return query.toString();
    }

    private void setDTO(DBConnectorTransferObject transferObject) {
        dto = transferObject;
    }
}


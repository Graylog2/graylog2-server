package org.graylog.integrations.dbconnector.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.graylog.integrations.dbconnector.DBConnectorProperty.CLOSE_DELIMITER;
import static org.graylog.integrations.dbconnector.DBConnectorProperty.COLUMN_QUERY;
import static org.graylog.integrations.dbconnector.DBConnectorProperty.COUNT_QUERY;
import static org.graylog.integrations.dbconnector.DBConnectorProperty.DATE_CONDITION;
import static org.graylog.integrations.dbconnector.DBConnectorProperty.DB2;
import static org.graylog.integrations.dbconnector.DBConnectorProperty.FROM;
import static org.graylog.integrations.dbconnector.DBConnectorProperty.LIMIT_CLAUSE;
import static org.graylog.integrations.dbconnector.DBConnectorProperty.LIMIT_RECORDS;
import static org.graylog.integrations.dbconnector.DBConnectorProperty.MYSQL;
import static org.graylog.integrations.dbconnector.DBConnectorProperty.OFFSET_CONDITION;
import static org.graylog.integrations.dbconnector.DBConnectorProperty.ORDER_BY;
import static org.graylog.integrations.dbconnector.DBConnectorProperty.POSTGRES;
import static org.graylog.integrations.dbconnector.DBConnectorProperty.RECORD_COUNT;
import static org.graylog.integrations.dbconnector.DBConnectorProperty.SELECT_QUERY;
import static org.graylog.integrations.dbconnector.DBConnectorProperty.TEST_QUERY;
import static org.graylog.integrations.dbconnector.DBConnectorProperty.TIMESTAMP;
import static org.graylog.integrations.dbconnector.DBConnectorProperty.WHERE_CLAUSE;

public class SQLClient implements DBConnectorClient {

    private static final Logger LOG = LoggerFactory.getLogger(SQLClient.class);
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
            int count = 1;
            while (rs.next()) {
                Map<Object, Object> map = new HashMap<>();
                for (int i = 1; i <= columnsNumber; i++) {
                    map.put(result.getColumnName(i), rs.getString(i));
                }
                count++;
                collection.put(count, map);
            }
        } catch (SQLException exception) {
            LOG.error("SQL Connection error occurred ", exception);
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
        if (collectQuery == null) collectQuery = prepareQuery();
        PreparedStatement stmt = getPreparedStatement(collectQuery);
        try {
            int rowCount = getRowCount();
            while (offset <= rowCount) {
                stmt.setMaxRows(BATCH_SIZE);
                stmt.setInt(2, BATCH_SIZE);
                stmt.setInt(3, offset);
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) listLogs.add(rs.getString(1));
                offset = offset + BATCH_SIZE;
            }
        } catch (SQLException exception) {
            LOG.error("SQL connection error: ", exception);
            throw new SQLException(exception);
        } finally {
            stmt.closeOnCompletion();
            connection.close();
        }
        return listLogs;
    }

    public String getColumns(String tableName) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement(COLUMN_QUERY);
        stmt.setString(1, tableName);
        ResultSet rs;
        StringBuilder columns = new StringBuilder();
        try {
            rs = stmt.executeQuery();
            columns = new StringBuilder();
            String col;
            while (rs.next()) {
                col = rs.getString(1);
                columns.append("'").append(col).append("'");
                columns.append(",").append(col).append(",");
            }
            columns.deleteCharAt(columns.length() - 1);
        } catch (SQLException e) {
            LOG.error("SQL Connection error ", e);
        }

        return "JSON_OBJECT(" + columns + ")";
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
            if (dto.databaseType().equals(MYSQL)) {
                return "`" + param + "`";
            }
        } else return param;
        return param;
    }

    public PreparedStatement getPreparedStatement(String query) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        if (dto.stateFieldType().equals(TIMESTAMP)) {
            stmt.setTimestamp(1, (Timestamp) dto.stateFieldValue());
        } else {
            stmt.setInt(1, Integer.parseInt(dto.stateFieldValue().toString()));
        }
        return stmt;
    }

    public String addQueryCondition(String stateField) {
        if (dto.stateFieldType().equals(TIMESTAMP)) {
            return WHERE_CLAUSE + patternCheck(stateField) + DATE_CONDITION;
        } else {
            return WHERE_CLAUSE + patternCheck(stateField) + OFFSET_CONDITION;
        }
    }

    public String prepareQuery() throws SQLException {
        String columns;
        StringBuilder query = new StringBuilder();
        switch (dto.databaseType()) {
            case MYSQL:
            case DB2:
                columns = getColumns(dto.tableName());
                query.append("select ").append(columns).append(FROM).append(patternCheck(dto.tableName())).append(addQueryCondition(dto.stateField())).append(ORDER_BY).append(dto.stateField()).append(LIMIT_CLAUSE).append(CLOSE_DELIMITER);
                break;
            case POSTGRES:
                query.append("select json_agg(t) from ").append(patternCheck(dto.tableName())).append(" as t").append(addQueryCondition(dto.stateField())).append(ORDER_BY).append(dto.stateField()).append(LIMIT_CLAUSE).append(CLOSE_DELIMITER);
                break;
            default:
                query.append(SELECT_QUERY).append(patternCheck(dto.tableName())).append(addQueryCondition(dto.stateField())).append(dto.stateField()).append(LIMIT_CLAUSE).append(" as json;");
                break;
        }
        return query.toString();
    }

    private void setDTO(DBConnectorTransferObject transferObject) {
        dto = transferObject;
    }
}

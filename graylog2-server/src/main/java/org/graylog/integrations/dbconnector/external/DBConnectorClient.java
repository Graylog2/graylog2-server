package org.graylog.integrations.dbconnector.external;

import com.fasterxml.jackson.databind.JsonNode;

import java.sql.SQLException;
import java.util.List;


public interface DBConnectorClient {

    void getConnection(String connectionString) throws Exception;

    JsonNode validateConnection(DBConnectorTransferObject transferObject) throws SQLException;

    List<String> fetchLogs(DBConnectorTransferObject transferObject) throws SQLException;


}

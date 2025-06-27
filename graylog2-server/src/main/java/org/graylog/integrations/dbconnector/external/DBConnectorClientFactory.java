package org.graylog.integrations.dbconnector.external;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.sql.SQLException;

import static org.graylog.integrations.dbconnector.DBConnectorProperty.DB2;
import static org.graylog.integrations.dbconnector.DBConnectorProperty.MONGODB;
import static org.graylog.integrations.dbconnector.DBConnectorProperty.MSSQL;
import static org.graylog.integrations.dbconnector.DBConnectorProperty.ORACLE;
import static org.graylog.integrations.dbconnector.DBConnectorProperty.POSTGRES;

public class DBConnectorClientFactory {
    private static final Logger LOG = LoggerFactory.getLogger(DBConnectorClientFactory.class);

    /**
     * Build Database Connector for a single connection string.
     *
     * @param dbType database type to identify it as SQL or NOSQL
     * @return DBConnectorClient instance based on DB type
     */
    public DBConnectorClient getClient(String dbType) throws MalformedURLException, SQLException, ClassNotFoundException {
        LOG.debug("Creating new Database connector client for {}", dbType);

        return switch (dbType) {
            case MONGODB -> new MongodbClient();
            case ORACLE -> new OracleClient();
            case MSSQL -> new MicrosoftSqlServerClient();
            case DB2 -> new DB2Client();
            case POSTGRES -> new PostgreSqlClient();
            default -> new SQLClient();
        };
    }
}

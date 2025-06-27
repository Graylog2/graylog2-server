package org.graylog.integrations.dbconnector;

public class DBConnectorUtils {
    public static String buildConnectionString(String dbType, String hostname, int port, String dbName,
                                         String username, String password) {
        return switch (dbType.toLowerCase()) {
            case "postgresql" -> String.format("jdbc:postgresql://%s:%s/%s?user=%s&password=%s",
                    hostname, port, dbName, username, password);
            case "mysql" -> String.format("jdbc:mysql://%s:%s/%s?user=%s&password=%s",
                    hostname, port, dbName, username, password);
            case "oracle" -> String.format("jdbc:oracle:thin:%s/%s@//%s:%s/%s",
                    username, password, hostname, port, dbName);
            case "sqlserver" -> String.format("jdbc:sqlserver://%s:%s;databaseName=%s;user=%s;password=%s",
                    hostname, port, dbName, username, password);
            case "db2" -> String.format("jdbc:db2://%s:%s/%s:user=%s;password=%s;",
                    hostname, port, dbName, username, password);
            case "mongodb" ->
                    String.format("mongodb://%s:%s/%s", hostname, port, dbName); // Optional: add user/pass if needed

            default -> throw new IllegalArgumentException("Unsupported DB type: " + dbType);
        };
    }
}

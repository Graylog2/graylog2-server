package org.graylog.integrations.dbconnector.api.requests;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.Nullable;

@JsonAutoDetect
public interface DBConnectorRequest {

    // Constants are defined here once for all classes.

    String DB_HOSTNAME = "hostname";
    String DB_PORT = "port";
    String DB_NAME = "database_name";
    String DB_USERNAME = "username";
    String DB_PASSWORD = "password";
    String DB_TABLE_NAME = "table_name";
    String DB_TYPE = "db_type";
    String MONGO_COLLECTION_NAME = "mongo_collection_name";

    @JsonProperty(DB_HOSTNAME)
    String hostname();

    @JsonProperty(DB_PORT)
    int port();

    @JsonProperty(DB_NAME)
    String dbName();

    @JsonProperty(DB_USERNAME)
    String username();

    @JsonProperty(DB_PASSWORD)
    String password();

    @Nullable
    @JsonProperty(DB_TABLE_NAME)
    String tableName();

    @JsonProperty(DB_TYPE)
    String dbType();

    @Nullable
    @JsonProperty(MONGO_COLLECTION_NAME)
    String mongoCollectionName();

    interface Builder<SELF> {

        @JsonProperty(DB_HOSTNAME)
        SELF hostname(String hostname);

        @JsonProperty(DB_PORT)
        SELF port(int port);

        @JsonProperty(DB_NAME)
        SELF dbName(String dbName);

        @JsonProperty(DB_USERNAME)
        SELF username(String username);

        @JsonProperty(DB_PASSWORD)
        SELF password(String password);

        @JsonProperty(DB_TABLE_NAME)
        SELF tableName(String tableName);

        @JsonProperty(MONGO_COLLECTION_NAME)
        SELF mongoCollectionName(String mongoCollectionName);

        @JsonProperty(DB_TYPE)
        SELF dbType(String dbType);

    }
}

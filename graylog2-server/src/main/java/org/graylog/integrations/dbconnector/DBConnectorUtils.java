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

import org.graylog.integrations.dbconnector.external.model.DBConnectorEndpoints;

import java.util.Locale;

public class DBConnectorUtils {
    public static String buildConnectionString(DBConnectorEndpoints dbType, String hostname, int port,
                                               String dbName, String username, String password) {
        return switch (dbType) {
            case POSTGRESQL -> String.format(Locale.ENGLISH,"jdbc:postgresql://%s:%d/%s?user=%s&password=%s",
                    hostname, port, dbName, username, password);

            case MYSQL -> String.format(Locale.ENGLISH,"jdbc:mysql://%s:%d/%s?user=%s&password=%s",
                    hostname, port, dbName, username, password);

            case ORACLE -> String.format(Locale.ENGLISH,"jdbc:oracle:thin:%s/%s@//%s:%d/%s",
                    username, password, hostname, port, dbName);

            case MICROSOFT_SQL -> String.format(Locale.ENGLISH,"jdbc:sqlserver://%s:%d;databaseName=%s;user=%s;password=%s",
                    hostname, port, dbName, username, password);

            case DB2 -> String.format(Locale.ENGLISH,"jdbc:db2://%s:%d/%s:user=%s;password=%s;",
                    hostname, port, dbName, username, password);

            case MONGODB -> String.format(Locale.ENGLISH,"mongodb://%s:%s@%s:%d/%s", username, password,
                    hostname, port, dbName);

        };
    }
}

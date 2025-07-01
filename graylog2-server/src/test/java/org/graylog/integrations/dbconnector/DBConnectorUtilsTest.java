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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class DBConnectorUtilsTest {

    private final String hostname = "localhost";
    private final int port = 5432;
    private final String dbName = "testdb";
    private final String username = "user";
    private final String password = "pass";

    @Test
    public void testPostgresConnectionString() {
        String expected = "jdbc:postgresql://localhost:5432/testdb?user=user&password=pass";
        String result = DBConnectorUtils.buildConnectionString(DBConnectorEndpoints.POSTGRESQL, hostname, port, dbName, username, password);
        assertEquals(expected, result);
    }

    @Test
    public void testMySQLConnectionString() {
        String expected = "jdbc:mysql://localhost:5432/testdb?user=user&password=pass";
        String result = DBConnectorUtils.buildConnectionString(DBConnectorEndpoints.MYSQL, hostname, port, dbName, username, password);
        assertEquals(expected, result);
    }

    @Test
    public void testOracleConnectionString() {
        String expected = "jdbc:oracle:thin:user/pass@//localhost:5432/testdb";
        String result = DBConnectorUtils.buildConnectionString(DBConnectorEndpoints.ORACLE, hostname, port, dbName, username, password);
        assertEquals(expected, result);
    }

    @Test
    public void testMSSQLConnectionString() {
        String expected = "jdbc:sqlserver://localhost:5432;databaseName=testdb;user=user;password=pass";
        String result = DBConnectorUtils.buildConnectionString(DBConnectorEndpoints.MICROSOFT_SQL, hostname, port, dbName, username, password);
        assertEquals(expected, result);
    }

    @Test
    public void testDB2ConnectionString() {
        String expected = "jdbc:db2://localhost:5432/testdb:user=user;password=pass;";
        String result = DBConnectorUtils.buildConnectionString(DBConnectorEndpoints.DB2, hostname, port, dbName, username, password);
        assertEquals(expected, result);
    }

    @Test
    public void testMongoDBConnectionString() {
        String expected = "mongodb://user:pass@localhost:5432/testdb";
        String result = DBConnectorUtils.buildConnectionString(DBConnectorEndpoints.MONGODB, hostname, port, dbName, username, password);
        assertEquals(expected, result);
    }

}

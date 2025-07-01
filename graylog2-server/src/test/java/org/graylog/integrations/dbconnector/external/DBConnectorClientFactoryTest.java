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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.graylog.integrations.dbconnector.DBConnectorProperty.DB2;
import static org.graylog.integrations.dbconnector.DBConnectorProperty.MONGODB;
import static org.graylog.integrations.dbconnector.DBConnectorProperty.MSSQL;
import static org.graylog.integrations.dbconnector.DBConnectorProperty.ORACLE;
import static org.graylog.integrations.dbconnector.DBConnectorProperty.POSTGRES;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class DBConnectorClientFactoryTest {

    private final DBConnectorClientFactory factory = new DBConnectorClientFactory();

    @Test
    public void testGetClient_MongoDB() throws Exception {
        DBConnectorClient client = factory.getClient(MONGODB);
        assertTrue(client instanceof MongodbClient);
    }

    @Test
    public void testGetClient_Oracle() throws Exception {
        DBConnectorClient client = factory.getClient(ORACLE);
        assertTrue(client instanceof OracleClient);
    }

    @Test
    public void testGetClient_MSSQL() throws Exception {
        DBConnectorClient client = factory.getClient(MSSQL);
        assertTrue(client instanceof MicrosoftSqlServerClient);
    }

    @Test
    public void testGetClient_DB2() throws Exception {
        DBConnectorClient client = factory.getClient(DB2);
        assertTrue(client instanceof DB2Client);
    }

    @Test
    public void testGetClient_Postgres() throws Exception {
        DBConnectorClient client = factory.getClient(POSTGRES);
        assertTrue(client instanceof PostgreSqlClient);
    }

    @Test
    public void testGetClient_Default() throws Exception {
        DBConnectorClient client = factory.getClient("unknown");
        assertTrue(client instanceof SQLClient);
    }
}

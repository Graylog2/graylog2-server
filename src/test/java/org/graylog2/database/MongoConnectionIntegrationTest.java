/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.graylog2.database;

import com.mongodb.DB;
import com.mongodb.Mongo;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author lennart
 */
public class MongoConnectionIntegrationTest {

    public MongoConnectionIntegrationTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of getInstance method, of class MongoConnection.
     */
    @Test
    public void testGetInstance() {
        assertNotNull(MongoConnection.getInstance());
    }

    /**
     * Test of connect method, of class MongoConnection.
     */
    @Test
    public void testConnect() throws Exception {
        MongoConnection instance = MongoConnection.getInstance();
        instance.connect(
            null,
            null,
            "localhost",
            "graylog2test",
            Integer.valueOf(27017),
            "false"
        );

        Mongo connection = instance.getConnection();
        assertNotNull(connection);
        assertEquals(connection.getConnectPoint(), "localhost:27017");
    }

}
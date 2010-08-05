/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.graylog2.periodical;

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
public class HostDistinctThreadTest {

    public HostDistinctThreadTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
        // connect to mongodb (test database)
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of run method, of class HostDistinctThread.
     */
    @Test
    public void testRun() {
        System.out.println("run");
        HostDistinctThread instance = new HostDistinctThread();
        instance.run();
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

}
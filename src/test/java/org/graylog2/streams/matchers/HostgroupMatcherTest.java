/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.graylog2.streams.matchers;

import org.graylog2.messagehandlers.gelf.GELFMessage;
import org.graylog2.streams.StreamRule;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author local
 */
public class HostgroupMatcherTest {

    public HostgroupMatcherTest() {
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
     * Test of match method, of class HostgroupMatcher.
     */
    @Test
    public void testMatch() {
        System.out.println("match");
        GELFMessage msg = null;
        StreamRule rule = null;
        HostgroupMatcher instance = new HostgroupMatcher();
        boolean expResult = false;
        boolean result = instance.match(msg, rule);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

}
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.graylog2.inputs.gelf;

import org.json.simple.JSONObject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author lennart.koopmann
 */
public class GELFProcessorTest {

    public GELFProcessorTest() {
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
     * Test of messageReceived method, of class GELFProcessor.
     */
    @Test
    public void testMessageReceived() throws Exception {
        System.out.println("messageReceived");
        GELFMessage message = null;
        GELFProcessor instance = new GELFProcessor();
        instance.messageReceived(message);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getJSON method, of class GELFProcessor.
     */
    @Test
    public void testGetJSON() {
        System.out.println("getJSON");
        String value = "";
        GELFProcessor instance = new GELFProcessor();
        JSONObject expResult = null;
        JSONObject result = instance.getJSON(value);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

}
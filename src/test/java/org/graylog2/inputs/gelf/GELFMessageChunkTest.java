/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.graylog2.inputs.gelf;

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
public class GELFMessageChunkTest {

    public GELFMessageChunkTest() {
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
     * Test of getArrival method, of class GELFMessageChunk.
     */
    @Test
    public void testGetArrival() {
        System.out.println("getArrival");
        GELFMessageChunk instance = null;
        int expResult = 0;
        int result = instance.getArrival();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getId method, of class GELFMessageChunk.
     */
    @Test
    public void testGetId() {
        System.out.println("getId");
        GELFMessageChunk instance = null;
        String expResult = "";
        String result = instance.getId();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getData method, of class GELFMessageChunk.
     */
    @Test
    public void testGetData() {
        System.out.println("getData");
        GELFMessageChunk instance = null;
        byte[] expResult = null;
        byte[] result = instance.getData();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getSequenceCount method, of class GELFMessageChunk.
     */
    @Test
    public void testGetSequenceCount() {
        System.out.println("getSequenceCount");
        GELFMessageChunk instance = null;
        int expResult = 0;
        int result = instance.getSequenceCount();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getSequenceNumber method, of class GELFMessageChunk.
     */
    @Test
    public void testGetSequenceNumber() {
        System.out.println("getSequenceNumber");
        GELFMessageChunk instance = null;
        int expResult = 0;
        int result = instance.getSequenceNumber();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of read method, of class GELFMessageChunk.
     */
    @Test
    public void testRead() throws Exception {
        System.out.println("read");
        GELFMessageChunk instance = null;
        instance.read();
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of toString method, of class GELFMessageChunk.
     */
    @Test
    public void testToString() {
        System.out.println("toString");
        GELFMessageChunk instance = null;
        String expResult = "";
        String result = instance.toString();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

}
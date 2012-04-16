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
public class GELFChunkManagerTest {

    public GELFChunkManagerTest() {
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
     * Test of run method, of class GELFChunkManager.
     */
    @Test
    public void testRun() {
        System.out.println("run");
        GELFChunkManager instance = new GELFChunkManager();
        instance.run();
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of isComplete method, of class GELFChunkManager.
     */
    @Test
    public void testIsComplete() {
        System.out.println("isComplete");
        String messageId = "";
        GELFChunkManager instance = new GELFChunkManager();
        boolean expResult = false;
        boolean result = instance.isComplete(messageId);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of isOutdated method, of class GELFChunkManager.
     */
    @Test
    public void testIsOutdated() {
        System.out.println("isOutdated");
        String messageId = "";
        GELFChunkManager instance = new GELFChunkManager();
        boolean expResult = false;
        boolean result = instance.isOutdated(messageId);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of dropMessage method, of class GELFChunkManager.
     */
    @Test
    public void testDropMessage() {
        System.out.println("dropMessage");
        String messageId = "";
        GELFChunkManager instance = new GELFChunkManager();
        instance.dropMessage(messageId);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of chunksToByteArray method, of class GELFChunkManager.
     */
    @Test
    public void testChunksToByteArray() throws Exception {
        System.out.println("chunksToByteArray");
        String messageId = "";
        GELFChunkManager instance = new GELFChunkManager();
        byte[] expResult = null;
        byte[] result = instance.chunksToByteArray(messageId);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of insert method, of class GELFChunkManager.
     */
    @Test
    public void testInsert() {
        System.out.println("insert");
        GELFMessageChunk chunk = null;
        GELFChunkManager instance = new GELFChunkManager();
        instance.insert(chunk);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of humanReadableChunkMap method, of class GELFChunkManager.
     */
    @Test
    public void testHumanReadableChunkMap() {
        System.out.println("humanReadableChunkMap");
        GELFChunkManager instance = new GELFChunkManager();
        String expResult = "";
        String result = instance.humanReadableChunkMap();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

}
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.graylog2.hostgroups;

import java.util.ArrayList;
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
public class HostgroupCacheTest {

    public HostgroupCacheTest() {
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
     * Test of getInstance method, of class HostgroupCache.
     */
    @Test
    public void testGetInstance() {
        System.out.println("getInstance");
        HostgroupCache expResult = null;
        HostgroupCache result = HostgroupCache.getInstance();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of get method, of class HostgroupCache.
     */
    @Test
    public void testGet() {
        System.out.println("get");
        HostgroupCache instance = null;
        ArrayList expResult = null;
        ArrayList result = instance.get();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of set method, of class HostgroupCache.
     */
    @Test
    public void testSet() {
        System.out.println("set");
        ArrayList<Hostgroup> groups = null;
        HostgroupCache instance = null;
        instance.set(groups);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

}
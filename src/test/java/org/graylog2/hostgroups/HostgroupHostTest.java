/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.graylog2.hostgroups;

import org.bson.types.ObjectId;
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
public class HostgroupHostTest {

    public HostgroupHostTest() {
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
     * Test of getObjectId method, of class HostgroupHost.
     */
    @Test
    public void testGetObjectId() {
        System.out.println("getObjectId");
        HostgroupHost instance = null;
        ObjectId expResult = null;
        ObjectId result = instance.getObjectId();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setObjectId method, of class HostgroupHost.
     */
    @Test
    public void testSetObjectId() {
        System.out.println("setObjectId");
        ObjectId objectId = null;
        HostgroupHost instance = null;
        instance.setObjectId(objectId);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getHostgroupId method, of class HostgroupHost.
     */
    @Test
    public void testGetHostgroupId() {
        System.out.println("getHostgroupId");
        HostgroupHost instance = null;
        int expResult = 0;
        int result = instance.getHostgroupId();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setHostgroupId method, of class HostgroupHost.
     */
    @Test
    public void testSetHostgroupId() {
        System.out.println("setHostgroupId");
        int hostgroupId = 0;
        HostgroupHost instance = null;
        instance.setHostgroupId(hostgroupId);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getType method, of class HostgroupHost.
     */
    @Test
    public void testGetType() {
        System.out.println("getType");
        HostgroupHost instance = null;
        int expResult = 0;
        int result = instance.getType();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setType method, of class HostgroupHost.
     */
    @Test
    public void testSetType() {
        System.out.println("setType");
        int type = 0;
        HostgroupHost instance = null;
        instance.setType(type);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getHostname method, of class HostgroupHost.
     */
    @Test
    public void testGetHostname() {
        System.out.println("getHostname");
        HostgroupHost instance = null;
        String expResult = "";
        String result = instance.getHostname();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setHostname method, of class HostgroupHost.
     */
    @Test
    public void testSetHostname() {
        System.out.println("setHostname");
        String hostname = "";
        HostgroupHost instance = null;
        instance.setHostname(hostname);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

}
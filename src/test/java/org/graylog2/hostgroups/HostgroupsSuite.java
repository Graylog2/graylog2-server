/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.graylog2.hostgroups;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 *
 * @author local
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({org.graylog2.hostgroups.HostgroupCacheTest.class,org.graylog2.hostgroups.HostgroupHostTest.class,org.graylog2.hostgroups.HostgroupTest.class})
public class HostgroupsSuite {

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

}
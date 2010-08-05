/**
 * Copyright 2010 Lennart Koopmann <lennart@scopeport.org>
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

/**
 * ToolsTest.java: Lennart Koopmann <lennart@scopeport.org> | Aug 5, 2010 6:49:52 PM
 */

package org.graylog2;

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
public class ToolsTest {

    public ToolsTest() {
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
     * Test of getPID method, of class Tools.
     */
    @Test
    public void testGetPID() throws Exception {
        String result = Tools.getPID();
        assertTrue(Integer.parseInt(result) > 0);
    }

    /**
     * Test of syslogLevelToReadable method, of class Tools.
     */
    @Test
    public void testSyslogLevelToReadable() {
        assertEquals(Tools.syslogLevelToReadable(1337), "Invalid");
        assertEquals(Tools.syslogLevelToReadable(0), "Emergency");
        assertEquals(Tools.syslogLevelToReadable(2), "Critical");
        assertEquals(Tools.syslogLevelToReadable(6), "Informational");
    }

    /**
     * Test of syslogFacilityToReadable method, of class Tools.
     */
    @Test
    public void testSyslogFacilityToReadable() {
        assertEquals(Tools.syslogFacilityToReadable(9001), "Unknown");
        assertEquals(Tools.syslogFacilityToReadable(0), "kernel");
        assertEquals(Tools.syslogFacilityToReadable(11), "FTP");
        assertEquals(Tools.syslogFacilityToReadable(22), "local6");
    }

    /**
     * Test of getSystemInformation method, of class Tools.
     */
    @Test
    public void testGetSystemInformation() {
        String result = Tools.getSystemInformation();
        assertTrue(result.trim().length() > 0);
    }

}
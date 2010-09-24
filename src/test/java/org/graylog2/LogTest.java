/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.graylog2;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

// This has to die. Use Log4j or another logging framework

/**
 *
 * @author lennart
 */
public class LogTest {

    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private final String logMessage = "testtestTEST";

    public LogTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
        System.setOut(new PrintStream(this.output));
    }

    @After
    public void tearDown() {

    }

    /**
     * Test of info method, of class Log. - Debug mode enabled.
     */
    @Test
    public void testInfoInDebug() {
        Main.debugMode = true;
        Log.info(this.logMessage);
        assertTrue(output.toString().contains(this.logMessage));
    }

    /**
     * Test of info method, of class Log. - Debug mode disabled.
     */
    @Test
    public void testInfoNotInDebug() {
        Main.debugMode = false;
        Log.info(this.logMessage);
        assertFalse(output.toString().contains(this.logMessage));
    }

    /**
     * Test of warn method, of class Log. - Debug mode enabled.
     */
    @Test
    public void testWarnDebug() {
        Main.debugMode = true;
        Log.warn(this.logMessage);
        assertTrue(output.toString().contains(this.logMessage));
    }

    /**
     * Test of warn method, of class Log. - Debug mode disabled.
     */
    @Test
    public void testWarnNotInDebug() {
        Main.debugMode = false;
        Log.warn(this.logMessage);
        assertFalse(output.toString().contains(this.logMessage));
    }

    /**
     * Test of crit method, of class Log. - Debug mode enabled.
     */
    @Test
    public void testCritInDebug() {
        Main.debugMode = true;
        Log.crit(this.logMessage);
        assertTrue(output.toString().contains(this.logMessage));
    }

    /**
     * Test of crit method, of class Log. - Debug mode disabled.
     */
    @Test
    public void testCritNotInDebug() {
        Main.debugMode = false;
        Log.crit(this.logMessage);
        assertFalse(output.toString().contains(this.logMessage));
    }

    /**
     * Test of emerg method, of class Log. - Debug mode enabled.
     */
    @Test
    public void testEmergInDebug() {
        Main.debugMode = true;
        Log.emerg(this.logMessage);
        assertTrue(output.toString().contains(this.logMessage));
    }

    /**
     * Test of emerg method, of class Log. - Debug mode disabled.
     */
    @Test
    public void testEmergNotInDebug() {
        Main.debugMode = false;
        Log.emerg(this.logMessage);
        assertFalse(output.toString().contains(this.logMessage));
    }

    /**
     * Test of toStdOut method, of class Log. - Debug mode enabled.
     */
    @Test
    public void testStdOutInDebug() {
        Main.debugMode = true;
        Log.toStdOut(this.logMessage, Log.SEVERITY_EMERG);
        assertTrue(output.toString().contains(this.logMessage));

        // Check if the correct severity is set.
        assertTrue(output.toString().contains(Log.severityToString(Log.SEVERITY_EMERG)));
    }

    /**
     * Test of info method, of class Log. - Debug mode disabled.
     */
    @Test
    public void testStdOutNotInDebug() {
        Main.debugMode = false;
        Log.toStdOut(this.logMessage, Log.SEVERITY_INFO);
        assertFalse(output.toString().contains(this.logMessage));
    }

    /**
     * Test of severityToString method, of class Log.
     */
    @Test
    public void testSeverityToString() {
        assertEquals(Log.severityToString(9001), "UNSPECIFIED");
        assertEquals(Log.severityToString(Log.SEVERITY_CRIT), "CRITICAL");
    }

}

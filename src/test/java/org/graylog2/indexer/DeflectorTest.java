/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.graylog2.indexer;

import org.graylog2.GraylogServerStub;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author lennart.koopmann
 */
public class DeflectorTest {

    @Test
    public void testExtractIndexNumber() {
        Deflector d = new Deflector(new GraylogServerStub());

        assertEquals(0, d.extractIndexNumber("graylog2_0"));
        assertEquals(4, d.extractIndexNumber("graylog2_4"));
        assertEquals(52, d.extractIndexNumber("graylog2_52"));
    }
    
    @Test
    public void testExtractIndexNumberWithCustomIndexPrefix() {
        Deflector d = new Deflector(new GraylogServerStub());

        assertEquals(0, d.extractIndexNumber("foo_0_bar_0"));
        assertEquals(4, d.extractIndexNumber("foo_0_bar_4"));
        assertEquals(52, d.extractIndexNumber("foo_0_bar_52"));
    }
    
    @Test(expected=NumberFormatException.class)
    public void testExtractIndexNumberWithMalformedFormatThrowsException() {
        Deflector d = new Deflector(new GraylogServerStub());

        assertEquals(0, d.extractIndexNumber("graylog2_hunderttausend"));
    }
    
    @Test
    public void testBuildIndexName() {
        Deflector d = new Deflector(new GraylogServerStub());

        assertEquals("graylog2_0", d.buildIndexName("graylog2", 0));
        assertEquals("graylog2_1", d.buildIndexName("graylog2", 1));
        assertEquals("graylog2_9001", d.buildIndexName("graylog2", 9001));
    }
    
}

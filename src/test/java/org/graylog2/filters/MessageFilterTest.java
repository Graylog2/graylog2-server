/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.graylog2.filters;

import org.graylog2.logmessage.LogMessage;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author lennart.koopmann
 */
public class MessageFilterTest {

    public MessageFilterTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Test
    public void testFilter() {
    }

    @Test
    public void testDiscardMessage() {
    }

    public class MessageFilterImpl implements MessageFilter {

        public LogMessage filter(LogMessage msg) {
            return null;
        }

        public boolean discardMessage() {
            return false;
        }
    }

}
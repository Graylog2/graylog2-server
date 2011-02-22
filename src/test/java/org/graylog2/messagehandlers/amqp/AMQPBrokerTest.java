/**
 * Copyright 2011 Lennart Koopmann <lennart@socketfeed.com>
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

package org.graylog2.messagehandlers.amqp;

import java.net.ConnectException;
import com.rabbitmq.client.Connection;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author lennart
 */
public class AMQPBrokerTest {

    protected final static String STANDARD_HOST  = "localhost";
    protected final static int STANDARD_PORT     = 5672;
    protected final static String STANDARD_USER  = "guest";
    protected final static String STANDARD_PASS  = "guest";
    protected final static String STANDARD_VHOST = "/";

    @Test
    public void testGetConnection() throws Exception {
        AMQPBroker instance = new AMQPBroker(null, 0, null, null, null);
        try {
            Connection connection = instance.getConnection();
        } catch (ConnectException e) {
            // That's okay.
        }
    }

    @Test
    public void testGetHostWithGivenParameter() {
        AMQPBroker instance = new AMQPBroker("bla.local", 0, null, null, null);
        assertEquals("bla.local", instance.getHost());
    }

    @Test
    public void testGetHostWithoutGivenParameter() {
        AMQPBroker instance = new AMQPBroker(null, 0, null, null, null);
        assertEquals(STANDARD_HOST, instance.getHost());
    }

    @Test
    public void testSetHost() {
        AMQPBroker instance = new AMQPBroker(null, 0, null, null, null);
        instance.setHost("foo.bar");
        assertEquals("foo.bar", instance.getHost());
    }

    @Test
    public void testSetHostWithNulltype() {
        AMQPBroker instance = new AMQPBroker(null, 0, null, null, null);
        assertEquals(STANDARD_HOST, instance.getHost());
    }

    @Test
    public void testGetPortWithGivenParameter() {
        AMQPBroker instance = new AMQPBroker(null, 15516, null, null, null);
        assertEquals(15516, instance.getPort());
    }

    @Test
    public void testGetPortWithoutGivenParameter() {
        AMQPBroker instance = new AMQPBroker(null, 0, null, null, null);
        assertEquals(STANDARD_PORT, instance.getPort());
    }

    @Test
    public void testSetPort() {
        AMQPBroker instance = new AMQPBroker(null, 0, null, null, null);
        instance.setPort(32432);
        assertEquals(32432, instance.getPort());
    }

    @Test
    public void testSetPortWithNulltype() {
        AMQPBroker instance = new AMQPBroker(null, 0, null, null, null);
        assertEquals(STANDARD_PORT, instance.getPort());
    }

    @Test
    public void testGetUsernameWithGivenParameter() {
        AMQPBroker instance = new AMQPBroker(null, 0, "mama", null, null);
        assertEquals("mama", instance.getUsername());
    }

    @Test
    public void testGetUsernameWithoutGivenParameter() {
        AMQPBroker instance = new AMQPBroker(null, 0, null, null, null);
        assertEquals(STANDARD_USER, instance.getUsername());
    }

    @Test
    public void testSetUsername() {
        AMQPBroker instance = new AMQPBroker(null, 0, null, null, null);
        instance.setUsername("foofoo");
        assertEquals("foofoo", instance.getUsername());
    }

    @Test
    public void testSetUsernameWithNulltype() {
        AMQPBroker instance = new AMQPBroker(null, 0, null, null, null);
        assertEquals(STANDARD_USER, instance.getUsername());
    }

    @Test
    public void testGetPasswordWithGivenParameter() {
        AMQPBroker instance = new AMQPBroker(null, 0, null, "totallysecret", null);
        assertEquals("totallysecret", instance.getPassword());
    }

    @Test
    public void testGetPasswordWithoutGivenParameter() {
        AMQPBroker instance = new AMQPBroker(null, 0, null, null, null);
        assertEquals(STANDARD_PASS, instance.getPassword());
    }

    @Test
    public void testSetPassword() {
        AMQPBroker instance = new AMQPBroker(null, 0, null, null, null);
        instance.setPassword("partygorilla");
        assertEquals("partygorilla", instance.getPassword());
    }

    public void testSetPasswordWithNulltype() {
        AMQPBroker instance = new AMQPBroker(null, 0, null, null, null);
        assertEquals(STANDARD_PASS, instance.getPassword());
    }

    @Test
    public void testGetVirtualHostWithGivenParameter() {
        AMQPBroker instance = new AMQPBroker(null, 0, null, null, "something");
        assertEquals("something", instance.getVirtualHost());
    }

    @Test
    public void testGetVirtualHostWithoutGivenParameter() {
        AMQPBroker instance = new AMQPBroker(null, 0, null, null, null);
        assertEquals(STANDARD_VHOST, instance.getVirtualHost());
    }

    @Test
    public void testSetVirtualHost() {
        AMQPBroker instance = new AMQPBroker(null, 0, null, null, null);
        instance.setVirtualHost("overtheocean");
        assertEquals("overtheocean", instance.getVirtualHost());
    }

    @Test
    public void testSetVirtualHostWithNulltype() {
        AMQPBroker instance = new AMQPBroker(null, 0, null, null, null);
        assertEquals(STANDARD_VHOST, instance.getVirtualHost());
    }
}
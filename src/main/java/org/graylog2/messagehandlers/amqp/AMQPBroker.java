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

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;

/**
 * AMQPBroker.java: Jan 20, 2011 7:58:26 PM
 *
 * Representing the AMQP broker. Has standard parameters to
 * connect to local AMQP broker with standard settings.
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public final class AMQPBroker {

    private String host = "localhost";
    private int port = 5672;
    private String username = "guest";
    private String password = "guest";
    private String virtualHost = "/";

    private Connection connection = null;

    public AMQPBroker(String host, int port, String username, String password, String virtualHost) {
        setHost(host);
        setPort(port);
        setUsername(username);
        setPassword(password);
        setVirtualHost(virtualHost);
    }

    public Connection getConnection() throws IOException {
        if (connection == null || !connection.isOpen()) {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setUsername(getUsername());
            factory.setPassword(getPassword());
            factory.setVirtualHost(getVirtualHost());
            factory.setHost(getHost());
            factory.setPort(getPort());
            this.connection = factory.newConnection();
        }

        return this.connection;
    }

    /**
     * @return the host
     */
    public String getHost() {
        return host;
    }

    /**
     * @param host the host to set
     */
    public void setHost(String host) {
        if (host != null && host.length() > 0) {
            this.host = host;
        }
    }

    /**
     * @return the port
     */
    public int getPort() {
        return port;
    }

    /**
     * @param port the port to set
     */
    public void setPort(int port) {
        if (port > 0) {
            this.port = port;
        }
    }

    /**
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * @param username the username to set
     */
    public void setUsername(String username) {
        if (username != null && username.length() > 0) {
            this.username = username;
        }
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param password the password to set
     */
    public void setPassword(String password) {
        if (password != null && password.length() > 0) {
            this.password = password;
        }
    }

    /**
     * @return the virtualHost
     */
    public String getVirtualHost() {
        return virtualHost;
    }

    /**
     * @param virtualHost the virtualHost to set
     */
    public void setVirtualHost(String virtualHost) {
        if (virtualHost != null && virtualHost.length() > 0) {
            this.virtualHost = virtualHost;
        }
    }

}

/**
 * Copyright 2012 Lennart Koopmann <lennart@socketfeed.com>
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

package org.graylog2.periodical;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.List;
import org.apache.log4j.Logger;
import org.graylog2.GraphiteFormatter;
import org.graylog2.GraylogServer;
import org.graylog2.MessageCounter;

/**
 * GraphiteWriterThread.java: 08.05.2012 16:13:29
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class GraphiteWriterThread implements Runnable {

    private static final Logger LOG = Logger.getLogger(GraphiteWriterThread.class);

    public static final String COUNTER_NAME = "graphitecounter";

    public static final int INITIAL_DELAY = 0;
    public static final int PERIOD = 1;

    private final GraylogServer graylogServer;

    String carbonHost;
    int carbonPort;

    public GraphiteWriterThread(GraylogServer graylogServer) {
        this.graylogServer = graylogServer;

        carbonHost = graylogServer.getConfiguration().getGraphiteCarbonHost();
        carbonPort = graylogServer.getConfiguration().getGraphiteCarbonTcpPort();
    }

    @Override
    public void run() {
        // Register message counter if it has not been done yet.
        if (this.graylogServer.getMessageCounterManager().get(COUNTER_NAME) == null) {
            this.graylogServer.getMessageCounterManager().register(COUNTER_NAME);
        }

        MessageCounter counter = this.graylogServer.getMessageCounterManager().get(COUNTER_NAME);
        try {
            GraphiteFormatter f = new GraphiteFormatter(counter);
            send(f.getAllMetrics());

            LOG.debug("Sent message counts to Graphite at <" + carbonHost + ":" + carbonPort + ">.");
        } catch (Exception e) {
            LOG.warn("Error in GraphiteWriterThread: " + e.getMessage(), e);
        } finally {
            counter.resetAllCounts();
        }
    }

    private boolean send(List<String> metrics) {
        try {
            Socket sock = new Socket(carbonHost, carbonPort);

            PrintWriter out = new PrintWriter(sock.getOutputStream(), true);

            for (String metric : metrics) {
                out.write(metric + "\n");
            }
            
            out.close();
            sock.close();
        } catch (SocketException e) {
            LOG.error("Could not send data to Graphite", e);
        } catch (UnknownHostException e) {
            LOG.error("Could not send data to Graphite (Unknown host: <" + carbonHost + ">)", e);
        } catch (IOException e) {
            LOG.error("Could not send data to Graphite (IO error):", e);
        }

        return true;
    }

}

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

import org.graylog2.Core;
import org.graylog2.metrics.LibratoMetricsFormatter;
import org.graylog2.plugin.Tools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import org.graylog2.streams.StreamImpl;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class LibratoMetricsWriterThread implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(LibratoMetricsWriterThread.class);

    public static final String COUNTER_NAME = "libratocounter";

    public static final int INITIAL_DELAY = 0;

    private final Core graylogServer;

    private static final String API_TARGET = "https://metrics-api.librato.com/v1/metrics";

    public LibratoMetricsWriterThread(Core graylogServer) {
        this.graylogServer = graylogServer;
    }

    @Override
    public void run() {

        // TODO

        /*
        // Register message counter if it has not been done yet.
        if (this.graylogServer.getMessageCounterManager().get(COUNTER_NAME) == null) {
            this.graylogServer.getMessageCounterManager().register(COUNTER_NAME);
        }

        MessageCounter counter = this.graylogServer.getMessageCounterManager().get(COUNTER_NAME);
        try {
            LibratoMetricsFormatter f = new LibratoMetricsFormatter(
                    counter,
                    graylogServer.getConfiguration().getLibratoMetricsPrefix(),
                    graylogServer.getConfiguration().getLibratoMetricsStreamFilter(),
                    graylogServer.getConfiguration().getLibratoMetricsHostsFilter(),
                    StreamImpl.nameMap(graylogServer)
            );

            send(f.asJson());

            LOG.debug("Sent message counts to Librato Metrics.");
        } catch (Exception e) {
            LOG.warn("Error in LibratoMetricsWriterThread: " + e.getMessage(), e);
        } finally {
            counter.resetAllCounts();
        }*/
    }

    private void send(String what) {
        HttpURLConnection connection = null;

        try {
            URL endpoint = new URL(API_TARGET);
            connection = (HttpURLConnection) endpoint.openConnection();

            connection.setRequestMethod("POST");

            String auth = graylogServer.getConfiguration().getLibratoMetricsAPIUser() + ":" + graylogServer.getConfiguration().getLibratoMetricsAPIToken();
            connection.setRequestProperty("Authorization", "Basic " + Tools.encodeBase64(auth));

            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("User-Agent", "Graylog2 LibratoMetrics writer");

            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);

            connection.setConnectTimeout(2000);
            connection.setReadTimeout(2000);

            // Send request
            DataOutputStream wr = new DataOutputStream (
            connection.getOutputStream ());
            wr.writeBytes(what);
            wr.flush();
            wr.close();

            if(connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                LOG.error("Could not write to Librato Metrics: Expected HTTP 200 but was {}", connection.getResponseCode());
            }
        } catch (Exception e) {
            LOG.error("Could not write to Librato Metrics.", e);
        } finally {
            // Make sure to close connection.
            if(connection != null) {
                connection.disconnect();
            }
        }

    }

}

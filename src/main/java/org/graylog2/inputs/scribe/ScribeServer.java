/**
 * Copyright 2011 Rackspace Hosting Inc.
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

package org.graylog2.inputs.scribe;

import org.apache.log4j.Logger;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TTransportException;
import org.apache.thrift.transport.TTransportFactory;
import org.graylog2.GraylogServer;
import scribe.Scribe;

import java.net.InetSocketAddress;

/**
 * ScribeServer.java:
 *
 * Implements the Thrift threadpool necessary to receive Scribe messages
 *
 */
public final class ScribeServer implements Runnable {

    private static final Logger LOG = Logger.getLogger(ScribeServer.class);

    TServerSocket server;

    private String host = "localhost";
    private int port = 5672;
    private final ScribeHandler handler;
    private final int rpcTimeout;
    private final int thriftLength;
    private final int minThreads;
    private final int maxThreads;

    public ScribeServer(GraylogServer graylogServer, String host, int port, int rpcTimeout,
                        int thriftLength, int minThreads, int maxThreads) {
        this.host = host;
        this.port = port;
        this.rpcTimeout = rpcTimeout;
        this.thriftLength = thriftLength;
        this.minThreads = minThreads;
        this.maxThreads = maxThreads;
        this.handler = new ScribeHandler(graylogServer);
    }

    @Override
    public void run() {
        LOG.info("Starting Scribe server on port :" + String.valueOf( this.port) );

        Scribe.Processor processor = new Scribe.Processor(handler);
        try {
            server = new TServerSocket(new InetSocketAddress(this.host, this.port),
                    rpcTimeout);
        } catch (TTransportException e) {
            throw new RuntimeException(String.format("Unable to create scribe server socket to %s:%s",
                                                     this.host, this.port), e);
        }

        // Protocol factory
        TProtocolFactory tProtocolFactory = new TBinaryProtocol.Factory(false,
                                                                        false,
                thriftLength);

        // Transport factory
        TTransportFactory inTransportFactory, outTransportFactory;
        int tFramedTransportSize = thriftLength;
        inTransportFactory  = new TFramedTransport.Factory(tFramedTransportSize);
        outTransportFactory = new TFramedTransport.Factory(tFramedTransportSize);
        LOG.info("Using TFastFramedTransport with a max frame size of " + String.valueOf( this.thriftLength) + " bytes");

        // ThreadPool Server
        TThreadPoolServer.Args args = new TThreadPoolServer.Args(server)
        .minWorkerThreads(minThreads)
        .maxWorkerThreads(maxThreads)
        .inputTransportFactory(inTransportFactory)
        .outputTransportFactory(outTransportFactory)
        .inputProtocolFactory(tProtocolFactory)
        .outputProtocolFactory(tProtocolFactory)
        .processor(processor);

        TThreadPoolServer ttps = new TThreadPoolServer(args);
        ttps.serve();
    }

    public String getHost() {
        return host;
    }

    private void setHost(String host) {
        if (host != null && host.length() > 0) {
            this.host = host;
        }
    }

    public int getPort() {
        return port;
    }

    private void setPort(int port) {
        if (port > 0) {
            this.port = port;
        }
    }
}

/*
 * Copyright 2012-2014 TORCH GmbH
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
 */

package org.graylog2.radio;

import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.name.Named;
import com.ning.http.client.AsyncHttpClient;
import org.graylog2.inputs.InputCache;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.Version;
import org.graylog2.radio.buffers.processors.RadioProcessBufferProcessor;
import org.graylog2.radio.cluster.Ping;
import org.graylog2.radio.transports.RadioTransport;
import org.graylog2.shared.ServerStatus;
import org.graylog2.shared.buffers.ProcessBuffer;
import org.graylog2.shared.buffers.ProcessBufferWatermark;
import org.graylog2.shared.buffers.processors.ProcessBufferProcessor;
import org.graylog2.shared.initializers.ServiceManagerListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class Radio {

    private static final Logger LOG = LoggerFactory.getLogger(Radio.class);

    @Inject
    private Configuration configuration;
    @Inject
    private ServerStatus serverStatus;
    @Inject
    @Named("scheduler")
    private ScheduledExecutorService scheduler;
    @Inject
    private InputCache inputCache;
    @Inject
    private ProcessBuffer processBuffer;

    @Inject
    private ProcessBufferWatermark processBufferWatermark;

    private Ping.Pinger pinger;

    @Inject
    private AsyncHttpClient httpClient;

    @Inject
    private ProcessBuffer.Factory processBufferFactory;
    @Inject
    private RadioProcessBufferProcessor.Factory processBufferProcessorFactory;
    @Inject
    private ServiceManager serviceManager;

    private final RadioTransport transport;
    private final ServiceManagerListener serviceManagerListener;

    @Inject
    public Radio(RadioTransport transport,
                 ServiceManagerListener serviceManagerListener) {
        this.transport = transport;
        this.serviceManagerListener = serviceManagerListener;
    }

    public void initialize() {
        int processBufferProcessorCount = configuration.getProcessBufferProcessors();

        ProcessBufferProcessor[] processors = new ProcessBufferProcessor[processBufferProcessorCount];

        for (int i = 0; i < processBufferProcessorCount; i++) {
            processors[i] = processBufferProcessorFactory.create(this.processBufferWatermark,
                    i,
                    processBufferProcessorCount,
                    transport);
        }

        processBuffer = processBufferFactory.create(inputCache, processBufferWatermark);
        processBuffer.initialize(processors, configuration.getRingSize(),
                configuration.getProcessorWaitStrategy(),
                configuration.getProcessBufferProcessors()
        );

        if (this.configuration.getRestTransportUri() == null) {
            String guessedIf;
            try {
                guessedIf = Tools.guessPrimaryNetworkAddress().getHostAddress();
            } catch (Exception e) {
                LOG.error("Could not guess primary network address for rest_transport_uri. Please configure it in your graylog2-radio.conf.", e);
                throw new RuntimeException("No rest_transport_uri.");
            }

            String transportStr = "http://" + guessedIf + ":" + configuration.getRestListenUri().getPort();
            LOG.info("No rest_transport_uri set. Falling back to [{}].", transportStr);
            this.configuration.setRestTransportUri(transportStr);
        }

/*        pinger = new Ping.Pinger(httpClient, serverStatus.getNodeId().toString(), configuration.getRestTransportUri(), configuration.getGraylog2ServerUri());
        startPings();*/

        serviceManager.addListener(serviceManagerListener, MoreExecutors.sameThreadExecutor());
        serviceManager.startAsync().awaitHealthy();


        // TODO: fix this.
        /*ThroughputCounterManagerThread tt = throughputCounterThreadFactory.create();
        scheduler.scheduleAtFixedRate(tt, 0, 1, TimeUnit.SECONDS);*/

        /*MasterCacheWorkerThread masterCacheWorker = new MasterCacheWorkerThread(this, inputCache, processBuffer);
        scheduler.scheduleAtFixedRate(masterCacheWorker, 0, 1, TimeUnit.SECONDS);*/
    }

    public void startPings() {
        // Start regular pings.
        scheduler.scheduleAtFixedRate(pinger, 0, 1, TimeUnit.SECONDS);
    }

    public void ping() {
        pinger.ping();
    }

    public void run() {
    }
}

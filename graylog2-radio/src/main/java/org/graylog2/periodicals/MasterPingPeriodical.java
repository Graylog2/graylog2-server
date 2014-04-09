package org.graylog2.periodicals;

import com.ning.http.client.AsyncHttpClient;
import org.graylog2.periodical.Periodical;
import org.graylog2.radio.Configuration;
import org.graylog2.radio.cluster.Ping;
import org.graylog2.shared.ServerStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class MasterPingPeriodical extends Periodical {
    private final Logger LOG = LoggerFactory.getLogger(MasterPingPeriodical.class);

    private final ServerStatus serverStatus;
    private final Configuration configuration;
    private final AsyncHttpClient httpClient;

    @Inject
    public MasterPingPeriodical(ServerStatus serverStatus,
                                Configuration configuration,
                                AsyncHttpClient httpClient) {
        this.serverStatus = serverStatus;
        this.configuration = configuration;
        this.httpClient = httpClient;
    }

    @Override
    public boolean runsForever() {
        return false;
    }

    @Override
    public boolean stopOnGracefulShutdown() {
        return true;
    }

    @Override
    public boolean masterOnly() {
        return false;
    }

    @Override
    public boolean startOnThisNode() {
        return true;
    }

    @Override
    public boolean isDaemon() {
        return false;
    }

    @Override
    public int getInitialDelaySeconds() {
        return 0;
    }

    @Override
    public int getPeriodSeconds() {
        return 1;
    }

    @Override
    public void run() {
        try {
            System.out.println("Pinging master ...");
            Ping.ping(httpClient,
                    configuration.getGraylog2ServerUri(),
                    configuration.getRestTransportUri(),
                    serverStatus.getNodeId().toString());
        } catch (IOException | ExecutionException | InterruptedException e) {
            System.out.println("Master ping failed.");
            e.printStackTrace();
            LOG.error("Master ping failed.", e);
        }
    }
}

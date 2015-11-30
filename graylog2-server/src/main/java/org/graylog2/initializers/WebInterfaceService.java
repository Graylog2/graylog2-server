package org.graylog2.initializers;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.glassfish.jersey.server.ContainerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.graylog2.jersey.container.netty.NettyContainer;
import org.graylog2.plugin.BaseConfiguration;
import org.graylog2.shared.initializers.AbstractJerseyService;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.handler.ssl.util.SelfSignedCertificate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import java.io.File;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.concurrent.Executor;

public class WebInterfaceService extends AbstractJerseyService {
    private static final Logger LOG = LoggerFactory.getLogger(WebInterfaceService.class);

    private final BaseConfiguration configuration;
    private final ServerBootstrap bootstrap;
    private final MetricRegistry metricRegistry;

    @Inject
    public WebInterfaceService(Provider<ObjectMapper> objectMapperProvider,
                               BaseConfiguration configuration,
                               MetricRegistry metricRegistry) {
        super(Collections.emptySet(), Collections.emptySet(), Collections.emptySet(), Collections.emptySet(), objectMapperProvider);
        this.configuration = configuration;
        this.metricRegistry = metricRegistry;
        this.bootstrap = buildServerBootStrap(
                instrumentedExecutor("boss-executor-service", "web-boss-%d", metricRegistry),
                instrumentedExecutor("worker-executor-service", "web-worker-%d", metricRegistry),
                configuration.getWebWorkerThreadsMaxPoolSize()
        );
    }

    @Override
    protected void startUp() throws Exception {
        final String[] resources = new String[] {"org.graylog2.web.resources"};
        final ResourceConfig rc = buildResourceConfig(
                configuration.isWebEnableGzip(),
                configuration.isWebEnableCors(),
                Collections.emptySet(),
                configuration.getWebListenUri(),
                resources
        );

        final NettyContainer jerseyHandler = ContainerFactory.createContainer(NettyContainer.class, rc);

        final int maxInitialLineLength = configuration.getWebMaxInitialLineLength();
        final int maxHeaderSize = configuration.getWebMaxHeaderSize();
        final int maxChunkSize = configuration.getWebMaxChunkSize();

        final File tlsCertFile;
        final File tlsKeyFile;
        if (configuration.isWebEnableTls() && (configuration.getWebTlsCertFile() == null || configuration.getWebTlsKeyFile() == null)) {
            final SelfSignedCertificate ssc = new SelfSignedCertificate(configuration.getWebListenUri().getHost());
            tlsCertFile = ssc.certificate();
            tlsKeyFile = ssc.privateKey();

            LOG.warn("web_tls_cert_file or web_tls_key_file is empty. Using self-signed certificates instead.");
            LOG.debug("web_tls_cert_file = {}", tlsCertFile);
            LOG.debug("web_tls_key_file = {}", tlsKeyFile);
        } else {
            tlsCertFile = configuration.getWebTlsCertFile();
            tlsKeyFile = configuration.getWebTlsKeyFile();
        }

        final Executor executor = buildPoolExecutor("web-execution-handler", configuration.getWebThreadPoolSize(), metricRegistry);

        bootstrap.setPipelineFactory(buildPipelineFactory(
                configuration.isWebEnableTls(),
                maxInitialLineLength,
                maxHeaderSize,
                maxChunkSize,
                executor,
                jerseyHandler,
                tlsCertFile,
                tlsKeyFile,
                configuration.getWebTlsKeyPassword()
        ));
        bootstrap.setOption("child.tcpNoDelay", true);
        bootstrap.setOption("child.keepAlive", true);

        bootstrap.bind(new InetSocketAddress(
                configuration.getWebListenUri().getHost(),
                configuration.getWebListenUri().getPort()
        ));

        LOG.info("Started Web Interface at <{}>", configuration.getWebListenUri());
    }

    @Override
    protected void shutDown() throws Exception {
        LOG.info("Shutting down Web Interface at <{}>", configuration.getWebListenUri());
        bootstrap.releaseExternalResources();
        bootstrap.shutdown();
    }
}

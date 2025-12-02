package org.graylog.aws;

import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.http.apache.ProxyConfiguration;

import java.net.URI;

@Singleton
public class AWSProxyConfigurationProvider implements Provider<ProxyConfiguration> {
    private static final Logger LOG = LoggerFactory.getLogger(AWSProxyConfigurationProvider.class);
    private final URI httpProxyUri;

    @Inject
    public AWSProxyConfigurationProvider(@Named("http_proxy_uri") @Nullable URI httpProxyUri) {
        this.httpProxyUri = httpProxyUri;
    }

    @Override
    public ProxyConfiguration get() {
        if (httpProxyUri == null) {
            LOG.debug("AWS proxy disabled: http_proxy_uri not set");
            return null;
        }
        ProxyConfiguration config = AWSProxyUtils.buildProxyConfiguration(httpProxyUri);
        LOG.debug("AWS proxy enabled: {}:{}", httpProxyUri.getHost(), httpProxyUri.getPort());
        return config;
    }
}

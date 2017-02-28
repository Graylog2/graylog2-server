/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.configuration;

import com.github.joschi.jadconfig.Parameter;
import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.ValidatorMethod;
import com.github.joschi.jadconfig.validators.PositiveIntegerValidator;
import com.github.joschi.jadconfig.validators.URIAbsoluteValidator;
import com.google.common.annotations.VisibleForTesting;
import org.graylog2.plugin.Tools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.graylog2.plugin.Tools.normalizeURI;

public class HttpConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(HttpConfiguration.class);

    private static final String WILDCARD_IP_ADDRESS = "0.0.0.0";
    private static final int GRAYLOG_DEFAULT_PORT = 9000;

    public static final String PATH_WEB = "";
    public static final String PATH_API = "/api/";

    @Parameter(value = "rest_listen_uri", required = true, validator = URIAbsoluteValidator.class)
    private URI restListenUri = URI.create("http://127.0.0.1:" + GRAYLOG_DEFAULT_PORT + PATH_API);

    @Parameter(value = "rest_transport_uri", validator = URIAbsoluteValidator.class)
    private URI restTransportUri;

    @Parameter(value = "rest_enable_cors")
    private boolean restEnableCors = true;

    @Parameter(value = "rest_enable_gzip")
    private boolean restEnableGzip = true;

    @Parameter(value = "rest_max_initial_line_length", required = true, validator = PositiveIntegerValidator.class)
    private int restMaxInitialLineLength = 4096;

    @Parameter(value = "rest_max_header_size", required = true, validator = PositiveIntegerValidator.class)
    private int restMaxHeaderSize = 8192;

    @Parameter(value = "rest_thread_pool_size", required = true, validator = PositiveIntegerValidator.class)
    private int restThreadPoolSize = 16;

    @Parameter(value = "rest_selector_runners_count", required = true, validator = PositiveIntegerValidator.class)
    private int restSelectorRunnersCount = 1;

    @Parameter(value = "rest_enable_tls")
    private boolean restEnableTls = false;

    @Parameter(value = "rest_tls_cert_file")
    private Path restTlsCertFile;

    @Parameter(value = "rest_tls_key_file")
    private Path restTlsKeyFile;

    @Parameter(value = "rest_tls_key_password")
    private String restTlsKeyPassword;

    @Parameter(value = "web_endpoint_uri")
    private URI webEndpointUri;

    public URI getRestListenUri() {
        return normalizeURI(restListenUri, getRestUriScheme(), GRAYLOG_DEFAULT_PORT, "/");
    }

    public String getRestUriScheme() {
        return getUriScheme(isRestEnableTls());
    }

    public String getUriScheme(boolean enableTls) {
        return enableTls ? "https" : "http";
    }

    public URI getRestTransportUri() {
        final URI defaultRestTransportUri = getDefaultRestTransportUri();
        if (restTransportUri == null) {
            LOG.debug("No rest_transport_uri set. Using default [{}].", defaultRestTransportUri);
            return defaultRestTransportUri;
        } else if (WILDCARD_IP_ADDRESS.equals(restTransportUri.getHost())) {
            LOG.warn("\"{}\" is not a valid setting for \"rest_transport_uri\". Using default [{}].", restTransportUri, defaultRestTransportUri);
            return defaultRestTransportUri;
        } else {
            return Tools.normalizeURI(restTransportUri, restTransportUri.getScheme(), GRAYLOG_DEFAULT_PORT, "/");
        }
    }

    public void setRestTransportUri(final URI restTransportUri) {
        this.restTransportUri = restTransportUri;
    }

    @VisibleForTesting
    protected URI getDefaultRestTransportUri() {
        final URI transportUri;
        final URI listenUri = getRestListenUri();

        if (WILDCARD_IP_ADDRESS.equals(listenUri.getHost())) {
            final InetAddress guessedAddress;
            try {
                guessedAddress = Tools.guessPrimaryNetworkAddress();

                if (guessedAddress.isLoopbackAddress()) {
                    LOG.debug("Using loopback address {}", guessedAddress);
                }
            } catch (Exception e) {
                LOG.error("Could not guess primary network address for \"rest_transport_uri\". Please configure it in your Graylog configuration.", e);
                throw new RuntimeException("No rest_transport_uri.", e);
            }

            try {
                transportUri = new URI(
                        listenUri.getScheme(),
                        listenUri.getUserInfo(),
                        guessedAddress.getHostAddress(),
                        listenUri.getPort(),
                        listenUri.getPath(),
                        listenUri.getQuery(),
                        listenUri.getFragment()
                );
            } catch (URISyntaxException e) {
                throw new RuntimeException("Invalid rest_transport_uri.", e);
            }
        } else {
            transportUri = listenUri;
        }

        return transportUri;
    }

    public boolean isRestEnableCors() {
        return restEnableCors;
    }

    public boolean isRestEnableGzip() {
        return restEnableGzip;
    }

    public int getRestMaxInitialLineLength() {
        return restMaxInitialLineLength;
    }

    public int getRestMaxHeaderSize() {
        return restMaxHeaderSize;
    }

    public int getRestThreadPoolSize() {
        return restThreadPoolSize;
    }

    public int getRestSelectorRunnersCount() {
        return restSelectorRunnersCount;
    }

    public boolean isRestEnableTls() {
        return restEnableTls;
    }

    public Path getRestTlsCertFile() {
        return restTlsCertFile;
    }

    public Path getRestTlsKeyFile() {
        return restTlsKeyFile;
    }

    public String getRestTlsKeyPassword() {
        return restTlsKeyPassword;
    }

    public URI getWebEndpointUri() {
        return webEndpointUri == null ? getRestTransportUri() : webEndpointUri;
    }

    @ValidatorMethod
    @SuppressWarnings("unused")
    public void validateRestTlsConfig() throws ValidationException {
        if (isRestEnableTls()) {
            if (!isRegularFileAndReadable(getRestTlsKeyFile())) {
                throw new ValidationException("Unreadable or missing REST API private key: " + getRestTlsKeyFile());
            }

            if (!isRegularFileAndReadable(getRestTlsCertFile())) {
                throw new ValidationException("Unreadable or missing REST API X.509 certificate: " + getRestTlsCertFile());
            }
        }
    }

    private boolean isRegularFileAndReadable(Path path) {
        return path != null && Files.isRegularFile(path) && Files.isReadable(path);
    }
}

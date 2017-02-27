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
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.graylog2.plugin.Tools.normalizeURI;

public class HttpConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(HttpConfiguration.class);

    private static final String WILDCARD_IP_ADDRESS = "0.0.0.0";
    private static final int GRAYLOG_DEFAULT_PORT = 9000;
    private static final int GRAYLOG_DEFAULT_WEB_PORT = 9000;

    @Parameter(value = "rest_listen_uri", required = true, validator = URIAbsoluteValidator.class)
    private URI restListenUri = URI.create("http://127.0.0.1:" + GRAYLOG_DEFAULT_PORT + "/api/");

    @Parameter(value = "rest_transport_uri", validator = URIAbsoluteValidator.class)
    private URI restTransportUri;

    @Parameter(value = "web_listen_uri", required = true, validator = URIAbsoluteValidator.class)
    private URI webListenUri = URI.create("http://127.0.0.1:" + GRAYLOG_DEFAULT_WEB_PORT + "/");


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

    @Parameter(value = "web_enable")
    private boolean webEnable = true;

    @Parameter(value = "web_endpoint_uri")
    private URI webEndpointUri;

    @Parameter(value = "web_enable_cors")
    private boolean webEnableCors = false;

    @Parameter(value = "web_enable_gzip")
    private boolean webEnableGzip = true;

    @Parameter(value = "web_max_initial_line_length", required = true, validator = PositiveIntegerValidator.class)
    private int webMaxInitialLineLength = 4096;

    @Parameter(value = "web_max_header_size", required = true, validator = PositiveIntegerValidator.class)
    private int webMaxHeaderSize = 8192;

    @Parameter(value = "web_enable_tls")
    private boolean webEnableTls = false;

    @Parameter(value = "web_thread_pool_size", required = true, validator = PositiveIntegerValidator.class)
    private int webThreadPoolSize = 16;

    @Parameter(value = "web_selector_runners_count", required = true, validator = PositiveIntegerValidator.class)
    private int webSelectorRunnersCount = 1;

    @Parameter(value = "web_tls_cert_file")
    private Path webTlsCertFile;

    @Parameter(value = "web_tls_key_file")
    private Path webTlsKeyFile;

    @Parameter(value = "web_tls_key_password")
    private String webTlsKeyPassword;

    public URI getRestListenUri() {
        return normalizeURI(restListenUri, getRestUriScheme(), GRAYLOG_DEFAULT_PORT, "/");
    }

    public URI getWebListenUri() {
        return normalizeURI(webListenUri, getWebUriScheme(), GRAYLOG_DEFAULT_WEB_PORT, "/");
    }

    public String getRestUriScheme() {
        return getUriScheme(isRestEnableTls());
    }

    public String getWebUriScheme() {
        return getUriScheme(isWebEnableTls());
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

    public boolean isWebEnable() {
        return webEnable;
    }

    public boolean isRestAndWebOnSamePort() {
        final URI restListenUri = getRestListenUri();
        final URI webListenUri = getWebListenUri();
        try {
            final InetAddress restAddress = InetAddress.getByName(restListenUri.getHost());
            final InetAddress webAddress = InetAddress.getByName(webListenUri.getHost());
            return restListenUri.getPort() == webListenUri.getPort() && restAddress.equals(webAddress);
        } catch (UnknownHostException e) {
            throw new RuntimeException("Unable to resolve hostnames of rest/web listen uris: ", e);
        }
    }

    public boolean isWebEnableCors() {
        return webEnableCors;
    }

    public boolean isWebEnableGzip() {
        return webEnableGzip;
    }

    public int getWebMaxInitialLineLength() {
        return webMaxInitialLineLength;
    }

    public int getWebMaxHeaderSize() {
        return webMaxHeaderSize;
    }

    public boolean isWebEnableTls() {
        return webEnableTls;
    }

    public int getWebThreadPoolSize() {
        return webThreadPoolSize;
    }

    public int getWebSelectorRunnersCount() {
        return webSelectorRunnersCount;
    }

    public Path getWebTlsCertFile() {
        return webTlsCertFile;
    }

    public Path getWebTlsKeyFile() {
        return webTlsKeyFile;
    }

    public String getWebTlsKeyPassword() {
        return webTlsKeyPassword;
    }

    public URI getWebEndpointUri() {
        return webEndpointUri == null ? getRestTransportUri() : webEndpointUri;
    }

    public String getWebPrefix() {
        final String webPrefix = getWebListenUri().getPath();
        if (webPrefix.endsWith("/")) {
            return webPrefix.substring(0, webPrefix.length() - 1);
        }
        return webPrefix;
    }

    @ValidatorMethod
    @SuppressWarnings("unused")
    public void validateRestTlsConfig() throws ValidationException {
        if(isRestEnableTls()) {
            if(!isRegularFileAndReadable(getRestTlsKeyFile())) {
                throw new ValidationException("Unreadable or missing REST API private key: " + getRestTlsKeyFile());
            }

            if(!isRegularFileAndReadable(getRestTlsCertFile())) {
                throw new ValidationException("Unreadable or missing REST API X.509 certificate: " + getRestTlsCertFile());
            }
        }
    }

    @ValidatorMethod
    @SuppressWarnings("unused")
    public void validateWebTlsConfig() throws ValidationException {
        if(isWebEnableTls() && !isRestAndWebOnSamePort()) {
            if(!isRegularFileAndReadable(getWebTlsKeyFile())) {
                throw new ValidationException("Unreadable or missing web interface private key: " + getWebTlsKeyFile());
            }

            if(!isRegularFileAndReadable(getWebTlsCertFile())) {
                throw new ValidationException("Unreadable or missing web interface X.509 certificate: " + getWebTlsCertFile());
            }
        }
    }

    @ValidatorMethod
    @SuppressWarnings("unused")
    public void validateRestAndWebListenConfigConflict() throws ValidationException {
        if (isRestAndWebOnSamePort() && getRestListenUri().getPath().equals(getWebListenUri().getPath())) {
            throw new ValidationException("If REST and Web interface are served on the same host/port, the path must be different!");
        }
    }

    @ValidatorMethod
    @SuppressWarnings("unused")
    public void validateWebAndRestHaveSameProtocolIfOnSamePort() throws ValidationException {
        if (isRestAndWebOnSamePort() && !getWebListenUri().getScheme().equals(getRestListenUri().getScheme())) {
            throw new ValidationException("If REST and Web interface are served on the same host/port, the protocols must be identical!");
        }
    }


    @ValidatorMethod
    @SuppressWarnings("unused")
    public void validateNetworkInterfaces() throws ValidationException {
        final URI restListenUri = getRestListenUri();
        final URI webListenUri = getWebListenUri();

        if (restListenUri.getPort() == webListenUri.getPort() &&
                !restListenUri.getHost().equals(webListenUri.getHost()) &&
                (WILDCARD_IP_ADDRESS.equals(restListenUri.getHost()) || WILDCARD_IP_ADDRESS.equals(webListenUri.getHost()))) {
            throw new ValidationException("Wildcard IP addresses cannot be used if the Graylog REST API and web interface listen on the same port.");
        }
    }

    private boolean isRegularFileAndReadable(Path path) {
        return path != null && Files.isRegularFile(path) && Files.isReadable(path);
    }
}

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
package org.graylog2.shared.security.tls;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.security.NoSuchAlgorithmException;
import java.util.Set;

public abstract class DefaultTLSProtocolProvider {
    // Defaults to TLS protocols that are currently considered secure
    public static final Set<String> DEFAULT_TLS_PROTOCOLS = ImmutableSet.of("TLSv1.2", "TLSv1.3");

    private static final Logger LOG = LoggerFactory.getLogger(DefaultTLSProtocolProvider.class);
    private static Set<String> defaultSupportedTlsProtocols = null;

    public synchronized static Set<String> getDefaultSupportedTlsProtocols() {
        if (defaultSupportedTlsProtocols != null) {
            return defaultSupportedTlsProtocols;
        }

        final Set<String> tlsProtocols = Sets.newHashSet(DEFAULT_TLS_PROTOCOLS);
        try {
            final Set<String> supportedProtocols = ImmutableSet.copyOf(SSLContext.getDefault().createSSLEngine().getEnabledProtocols());
            if (tlsProtocols.retainAll(supportedProtocols)) {
                LOG.warn("JRE doesn't support all default TLS protocols. Changing <{}> to <{}>", DEFAULT_TLS_PROTOCOLS, tlsProtocols);
            }
            defaultSupportedTlsProtocols = tlsProtocols;
        } catch (NoSuchAlgorithmException e) {
            LOG.error("Failed to detect supported TLS protocols. Keeping default <{}>", DEFAULT_TLS_PROTOCOLS, e);
            defaultSupportedTlsProtocols = DEFAULT_TLS_PROTOCOLS;
        }
        return defaultSupportedTlsProtocols;
    }
}

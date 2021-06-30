package org.graylog2.configuration;

import com.github.joschi.jadconfig.Parameter;
import com.github.joschi.jadconfig.converters.StringSetConverter;
import org.graylog2.shared.security.tls.DefaultTLSProtocolProvider;

import java.util.Set;

/**
 * Configuration bean for enabled TLS protocols.
 *
 * This was extracted to a separate configuration bean
 * so that it can be parsed in the early server startup phase individually.
 * Parsing the entire server configuration might trigger the initialization of the default SSLContext,
 * which needs to happen after a `jdk.tls.disabledAlgorithms` setting is applied.
 */
public class TLSProtocolsConfiguration {
    @Parameter(value = "enabled_tls_protocols", converter = StringSetConverter.class)
    private Set<String> enabledTlsProtocols = null;

    public Set<String> getConfiguredTlsProtocols() {
        return enabledTlsProtocols;
    }

    /**
     * Retrieve the enabled TLS protocols setting.
     * @return
     * If the setting is explicitly configured (not null) return that.
     * If it's configured to an empty set, return all supported protocols by the JVM.
     * If it's not configured (null and the default) return a secure set of supported TLS protocols.
     */
    public Set<String> getEnabledTlsProtocols() {
        if (enabledTlsProtocols != null) {
            if (enabledTlsProtocols.isEmpty()) {
                return DefaultTLSProtocolProvider.getSupportedTlsProtocols();
            }
            return enabledTlsProtocols;
        }
        return DefaultTLSProtocolProvider.getDefaultSupportedTlsProtocols();
    }
}

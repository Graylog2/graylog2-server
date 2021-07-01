/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.configuration;

import com.github.joschi.jadconfig.Parameter;
import com.github.joschi.jadconfig.converters.StringSetConverter;
import org.graylog2.shared.security.tls.DefaultTLSProtocolProvider;

import javax.annotation.Nullable;
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

    public TLSProtocolsConfiguration() {
    }

    /**
     * Used to transfer this setting from {@link org.graylog2.Configuration}
     */
    public TLSProtocolsConfiguration(Set<String> enabledTlsProtocols) {
        this.enabledTlsProtocols = enabledTlsProtocols;
    }

    /**
     * Used to access the plain configuration value.
     * In most cases you'd want to use {@link TLSProtocolsConfiguration#getEnabledTlsProtocols()}
     */
    @Nullable
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
        return getEnabledTlsProtocols(enabledTlsProtocols);
    }

    public static Set<String> getEnabledTlsProtocols(Set<String> configuredTlsProtocols) {
        if (configuredTlsProtocols != null) {
            if (configuredTlsProtocols.isEmpty()) {
                return DefaultTLSProtocolProvider.getSupportedTlsProtocols();
            }
            return configuredTlsProtocols;
        }
        return DefaultTLSProtocolProvider.getDefaultSupportedTlsProtocols();
    }
}

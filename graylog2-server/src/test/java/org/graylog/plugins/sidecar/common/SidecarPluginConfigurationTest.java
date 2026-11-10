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
package org.graylog.plugins.sidecar.common;

import com.github.joschi.jadconfig.JadConfig;
import com.github.joschi.jadconfig.RepositoryException;
import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.repositories.InMemoryRepository;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class SidecarPluginConfigurationTest {

    private SidecarPluginConfiguration configFrom(Map<String, String> properties)
            throws RepositoryException, ValidationException {
        final SidecarPluginConfiguration config = new SidecarPluginConfiguration();
        new JadConfig(new InMemoryRepository(properties), config).process();
        return config;
    }

    @Test
    void defaultsMatchTheGraylogSidecarPackages() throws Exception {
        final SidecarPluginConfiguration config = configFrom(Map.of());

        assertThat(config.getUser()).isEqualTo("graylog-sidecar");
        assertThat(config.getCollectorBinaryDir()).isEqualTo("/usr/lib/graylog-sidecar");
        assertThat(config.getSpoolDir()).isEqualTo("/var/lib/graylog-sidecar");
        assertThat(config.getWindowsInstallDir()).isEqualTo("C:\\Program Files\\Graylog\\sidecar");
        assertThat(config.getServerConfigDir()).isEqualTo("/etc/graylog/server");
        assertThat(config.getHostVariable()).isEqualTo("graylog_host");
    }

    @Test
    void pathsAndUserAreConfigurable() throws Exception {
        final Map<String, String> rawConfig = Map.of(
                "sidecar_user", "custom-sidecar",
                "sidecar_collector_binary_dir", "/usr/lib/custom-sidecar",
                "sidecar_spool_dir", "/var/lib/custom-sidecar",
                "sidecar_windows_install_dir", "C:\\Program Files\\custom\\sidecar",
                "sidecar_server_config_dir", "/etc/custom/server"
        );

        final SidecarPluginConfiguration config = configFrom(rawConfig);

        assertThat(config.getUser()).isEqualTo(rawConfig.get("sidecar_user"));
        assertThat(config.getCollectorBinaryDir()).isEqualTo(rawConfig.get("sidecar_collector_binary_dir"));
        assertThat(config.getSpoolDir()).isEqualTo(rawConfig.get("sidecar_spool_dir"));
        assertThat(config.getWindowsInstallDir()).isEqualTo(rawConfig.get("sidecar_windows_install_dir"));
        assertThat(config.getServerConfigDir()).isEqualTo(rawConfig.get("sidecar_server_config_dir"));
    }

    @Test
    void hostVariableIsConfigurable() throws Exception {
        final SidecarPluginConfiguration config = configFrom(Map.of("sidecar_host_variable", "custom_host"));

        assertThat(config.getHostVariable()).isEqualTo("custom_host");
    }

    @Test
    void rejectsInvalidHostVariableNames() {
        assertThat(catchThrowable(() -> configFrom(Map.of("sidecar_host_variable", "my host"))))
                .isInstanceOf(ValidationException.class);
        assertThat(catchThrowable(() -> configFrom(Map.of("sidecar_host_variable", "1host"))))
                .isInstanceOf(ValidationException.class);
        assertThat(catchThrowable(() -> configFrom(Map.of("sidecar_host_variable", "my-host"))))
                .isInstanceOf(ValidationException.class);
    }
}

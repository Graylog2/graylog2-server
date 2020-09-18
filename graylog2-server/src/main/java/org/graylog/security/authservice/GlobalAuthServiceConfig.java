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
package org.graylog.security.authservice;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog2.plugin.cluster.ClusterConfigService;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public class GlobalAuthServiceConfig {
    private final ClusterConfigService clusterConfigService;
    private final DBAuthServiceBackendService dbService;
    private final Map<String, AuthServiceBackend.Factory<? extends AuthServiceBackend>> backendFactories;
    private final AuthServiceBackend defaultBackend;

    @Inject
    public GlobalAuthServiceConfig(ClusterConfigService clusterConfigService,
                                   DBAuthServiceBackendService dbService,
                                   Map<String, AuthServiceBackend.Factory<? extends AuthServiceBackend>> backendFactories,
                                   @InternalAuthServiceBackend AuthServiceBackend defaultBackend) {
        this.clusterConfigService = clusterConfigService;
        this.dbService = dbService;
        this.backendFactories = backendFactories;
        this.defaultBackend = requireNonNull(defaultBackend, "defaultBackend cannot be null");
    }

    public AuthServiceBackend getDefaultBackend() {
        return defaultBackend;
    }

    public Optional<AuthServiceBackend> getActiveBackend() {
        final Data data = clusterConfigService.get(Data.class);
        if (data == null) {
            return Optional.empty();
        }

        return data.activeBackend()
                .flatMap(dbService::get)
                .flatMap(this::createBackend);
    }

    private Optional<AuthServiceBackend> createBackend(AuthServiceBackendDTO backend) {
        return Optional.ofNullable(backendFactories.get(backend.config().type()))
                .map(factory -> factory.create(backend));
    }

    public Optional<AuthServiceBackendDTO> getActiveBackendConfig() {
        final Data data = clusterConfigService.getOrDefault(Data.class, Data.create(null));

        return data.activeBackend().flatMap(dbService::get);
    }

    public Data getConfiguration() {
        return clusterConfigService.getOrDefault(Data.class, Data.create(null));
    }

    public Data updateConfiguration(Data updatedData) {
        clusterConfigService.write(updatedData);
        return requireNonNull(clusterConfigService.get(Data.class), "updated configuration cannot be null");
    }

    @AutoValue
    public static abstract class Data {
        @JsonProperty("active_backend")
        public abstract Optional<String> activeBackend();

        @JsonCreator
        public static GlobalAuthServiceConfig.Data create(@JsonProperty("active_backend") @Nullable String activeBackend) {
            return new AutoValue_GlobalAuthServiceConfig_Data(Optional.ofNullable(activeBackend));
        }
    }
}

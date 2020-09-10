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

import javax.inject.Inject;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public class GlobalAuthServiceConfig {
    private final ClusterConfigService clusterConfigService;
    private final AuthServiceBackend defaultBackend;

    @Inject
    public GlobalAuthServiceConfig(ClusterConfigService clusterConfigService,
                                   @InternalAuthServiceBackend AuthServiceBackend defaultBackend) {
        this.clusterConfigService = clusterConfigService;
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
        return Optional.empty(); // TODO: Load and return the actual backend
    }

    @AutoValue
    public static abstract class Data {
        @JsonProperty("active_backend")
        public abstract String activeBackend();

        @JsonCreator
        public static GlobalAuthServiceConfig.Data create(@JsonProperty("active_backend") String activeBackend) {
            return new AutoValue_GlobalAuthServiceConfig_Data(activeBackend);
        }
    }
}

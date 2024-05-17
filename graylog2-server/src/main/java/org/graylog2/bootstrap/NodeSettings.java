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
package org.graylog2.bootstrap;

import com.google.auto.value.AutoValue;
import org.graylog2.plugin.ServerStatus;

import java.util.Set;

@AutoValue
public abstract class NodeSettings {

    public abstract String name();

    // todo change this to a list of enabled options, this way we could do validation of dependent options here directly

    public abstract boolean withPlugins();

    public abstract boolean withMongoDb();

    public abstract boolean withEventBus();

    public abstract boolean withScheduler();

    public abstract boolean withTlsAndJettyNativeConfigured();

    public abstract Set<ServerStatus.Capability> capabilities();

    public abstract String defaultConfigFile();

    public abstract String defaultFeatureFlagFile();

    public static NodeSettings minimalNode(String name) {
        return minimalNodeBuilder(name).build();
    }

    public static Builder minimalNodeBuilder(String name) {
        Builder builder = NodeSettings.builder()
                .name(name)
                .withPlugins(false)
                .withMongoDb(false)
                .withEventBus(false)
                .withScheduler(false)
                .withTlsAndJettyNativeConfigured(false)
                .capabilities(Set.of());
        return builder;
    }

    public static NodeSettings fullNode(String name) {
        Builder builder = fullNodeBuilder(name);
        return builder
                .build();
    }

    public static Builder fullNodeBuilder(String name) {
        Builder builder = NodeSettings.builder()
                .name(name)
                .withPlugins(true)
                .withMongoDb(true)
                .withEventBus(true)
                .withScheduler(true)
                .withTlsAndJettyNativeConfigured(true)
                .capabilities(Set.of());
        return builder;
    }

    public static Builder builder() {
        return new AutoValue_NodeSettings.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder name(String name);

        public abstract Builder withPlugins(boolean withPlugins);

        public abstract Builder withMongoDb(boolean withMongoDb);

        public abstract Builder withEventBus(boolean withEventBus);

        public abstract Builder withScheduler(boolean withScheduler);

        public abstract Builder withTlsAndJettyNativeConfigured(boolean withTlsAndJettyNativeConfigured);

        public abstract Builder capabilities(Set<ServerStatus.Capability> capabilities);

        public abstract Builder defaultConfigFile(String defaultConfigFile);

        public abstract Builder defaultFeatureFlagFile(String defaultFeatureFlagFile);

        public abstract NodeSettings build();
    }

}

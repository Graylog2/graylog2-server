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

    public abstract boolean withPlugins();

    public abstract boolean withMongoDb();

    public abstract boolean withEventBus();

    public abstract Set<ServerStatus.Capability> capabilities();

    public static Builder builder() {
        return new AutoValue_NodeSettings.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder withPlugins(boolean withPlugins);

        public abstract Builder capabilities(Set<ServerStatus.Capability> capabilities);

        public abstract Builder withMongoDb(boolean withMongoDb);

        public abstract Builder withEventBus(boolean withEventBus);

        public abstract NodeSettings build();
    }
}

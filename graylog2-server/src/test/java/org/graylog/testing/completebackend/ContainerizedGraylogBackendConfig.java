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
package org.graylog.testing.completebackend;

import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import org.graylog.testing.mongodb.MongoDBVersion;
import org.graylog2.storage.SearchVersion;

import java.util.List;
import java.util.Map;

@AutoValue
public abstract class ContainerizedGraylogBackendConfig {
    public abstract Lifecycle lifecycle();

    public abstract GraylogServerProduct serverProduct();

    public abstract GraylogDataNodeProduct datanodeProduct();

    public abstract SearchVersion searchServerVersion();

    public abstract MongoDBVersion mongoDBVersion();

    public abstract List<String> enabledFeatureFlags();

    public abstract Map<String, String> env();

    public abstract boolean importLicenses();

    @Memoized
    public ContainerizedGraylogBackendServicesProvider serviceProvider() {
        return new ContainerizedGraylogBackendServicesProvider(lifecycle());
    }

    public static Builder builder() {
        return Builder.create();
    }

    @AutoValue.Builder
    public static abstract class Builder {
        public static Builder create() {
            return new AutoValue_ContainerizedGraylogBackendConfig.Builder();
        }

        public abstract Builder lifecycle(Lifecycle lifecycle);

        public abstract Builder serverProduct(GraylogServerProduct serverProduct);

        public abstract Builder datanodeProduct(GraylogDataNodeProduct datanodeProduct);

        public abstract Builder searchServerVersion(SearchVersion searchServerVersion);

        public abstract Builder mongoDBVersion(MongoDBVersion mongoDBVersion);

        public abstract Builder enabledFeatureFlags(List<String> enabledFeatureFlags);

        public abstract Builder env(Map<String, String> env);

        public abstract Builder importLicenses(boolean importLicenses);

        public abstract ContainerizedGraylogBackendConfig build();
    }
}

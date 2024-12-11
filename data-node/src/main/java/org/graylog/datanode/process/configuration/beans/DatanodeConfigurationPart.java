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
package org.graylog.datanode.process.configuration.beans;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import jakarta.annotation.Nullable;
import org.graylog.datanode.process.configuration.files.DatanodeConfigFile;
import org.graylog.security.certutil.csr.KeystoreInformation;

import java.security.KeyStore;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@AutoValue
public abstract class DatanodeConfigurationPart {
    public abstract List<String> nodeRoles();

    public abstract Map<String, String> keystoreItems();

    public abstract Map<String, String> properties();

    public abstract List<String> javaOpts();

    public abstract Map<String, String> systemProperties();

    @Nullable
    public abstract KeystoreInformation httpCertificate();

    @Nullable
    public abstract KeystoreInformation transportCertificate();

    public abstract boolean securityConfigured();

    @Nullable
    public abstract KeyStore trustStore();

    public abstract List<DatanodeConfigFile> configFiles();

    public static Builder builder() {
        return new AutoValue_DatanodeConfigurationPart.Builder()
                .nodeRoles(Collections.emptyList())
                .keystoreItems(Collections.emptyMap())
                .properties(Collections.emptyMap())
                .javaOpts(Collections.emptyList())
                .configFiles(Collections.emptyList())
                .securityConfigured(false)
                .trustStore(null)
                .systemProperties(Collections.emptyMap());
    }

    @AutoValue.Builder
    public abstract static class Builder {

        public abstract Builder nodeRoles(List<String> nodeRoles);

        abstract ImmutableList.Builder<String> nodeRolesBuilder();

        public final Builder addNodeRole(String role) {
            nodeRolesBuilder().add(role);
            return this;
        }

        public abstract Builder javaOpts(List<String> javaOpts);

        abstract ImmutableList.Builder<String> javaOptsBuilder();

        public final Builder javaOpt(String opt) {
            javaOptsBuilder().add(opt);
            return this;
        }

        public abstract Builder configFiles(List<DatanodeConfigFile> configFiles);

        abstract ImmutableList.Builder<DatanodeConfigFile> configFilesBuilder();

        public Builder withConfigFile(DatanodeConfigFile configFile) {
            configFilesBuilder().add(configFile);
            return this;
        }

        public abstract Builder keystoreItems(Map<String, String> keystoreItems);

        public abstract Builder properties(Map<String, String> properties);

        public abstract Builder httpCertificate(KeystoreInformation httpCertificate);

        public abstract Builder transportCertificate(KeystoreInformation httpCertificate);

        @Deprecated
        public abstract Builder securityConfigured(boolean securityConfigured);

        public abstract Builder trustStore(@Nullable KeyStore truststore);


        private final ImmutableMap.Builder<String, String> systemPropertiesBuilder = ImmutableMap.builder();

        ImmutableMap.Builder<String, String> systemPropertiesBuilder() {
            return systemPropertiesBuilder;
        }

        abstract Builder systemProperties(Map<String, String> systemProperties); // not public

        abstract DatanodeConfigurationPart autoBuild(); // not public

        public DatanodeConfigurationPart build() {
            systemProperties(systemPropertiesBuilder.buildKeepingLast());
            return autoBuild();
        }

        public Builder systemProperty(String key, String value) {
            systemPropertiesBuilder().put(key, value);
            return this;
        }
    }
}

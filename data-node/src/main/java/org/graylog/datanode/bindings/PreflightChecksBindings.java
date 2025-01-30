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
package org.graylog.datanode.bindings;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import org.graylog.datanode.bootstrap.preflight.DatanodeDirectoriesLockfileCheck;
import org.graylog.datanode.bootstrap.preflight.DatanodeDnsPreflightCheck;
import org.graylog.datanode.bootstrap.preflight.DatanodeKeystoreCheck;
import org.graylog.datanode.bootstrap.preflight.OpenSearchPreconditionsCheck;
import org.graylog.datanode.bootstrap.preflight.OpensearchBinPreflightCheck;
import org.graylog.datanode.bootstrap.preflight.OpensearchDataDirCompatibilityCheck;
import org.graylog.datanode.opensearch.CsrRequester;
import org.graylog.datanode.opensearch.CsrRequesterImpl;
import org.graylog.grn.GRNRegistry;
import org.graylog2.bindings.providers.MongoConnectionProvider;
import org.graylog2.bootstrap.preflight.MongoDBPreflightCheck;
import org.graylog2.bootstrap.preflight.PasswordSecretPreflightCheck;
import org.graylog2.bootstrap.preflight.PreflightCheck;
import org.graylog2.cluster.certificates.CertificateExchange;
import org.graylog2.cluster.certificates.CertificateExchangeImpl;
import org.graylog2.database.MongoConnection;
import org.graylog2.featureflag.FeatureFlags;
import org.graylog2.jackson.InputConfigurationBeanDeserializerModifier;
import org.graylog2.plugin.inject.JacksonSubTypes;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.shared.plugins.ChainingClassLoader;
import org.graylog2.shared.plugins.GraylogClassLoader;

import java.util.Collections;
import java.util.Set;

public class PreflightChecksBindings extends AbstractModule {


    private final ChainingClassLoader chainingClassLoader;
    private final FeatureFlags featureFlags;

    public PreflightChecksBindings(ChainingClassLoader chainingClassLoader, FeatureFlags featureFlags) {
        this.chainingClassLoader = chainingClassLoader;
        this.featureFlags = featureFlags;
    }

    @Override
    protected void configure() {
        bind(CsrRequester.class).to(CsrRequesterImpl.class).asEagerSingleton();
        bind(CertificateExchange.class).to(CertificateExchangeImpl.class);

        addPreflightCheck(MongoDBPreflightCheck.class);
        addPreflightCheck(DatanodeDnsPreflightCheck.class);
        addPreflightCheck(OpensearchBinPreflightCheck.class);
        addPreflightCheck(DatanodeDirectoriesLockfileCheck.class);
        addPreflightCheck(OpenSearchPreconditionsCheck.class);
        addPreflightCheck(OpensearchDataDirCompatibilityCheck.class);
        addPreflightCheck(PasswordSecretPreflightCheck.class);

        bindLimitedObjectMapper();

        // Mongodb is needed for legacy datanode storage, where we want to extract the certificate chain from
        // mongodb and store it in local keystore
        bind(MongoConnection.class).toProvider(MongoConnectionProvider.class);
        addPreflightCheck(DatanodeKeystoreCheck.class);
    }

    /**
     * TODO:
     * Is there a way to avoid all of this in the very limited preflight check bindings?
     * The whole purpose of this is to create an object mapper that's able to de/serialize EncryptedValues.
     */
    private void bindLimitedObjectMapper() {
        bind(GRNRegistry.class).toInstance(GRNRegistry.createWithBuiltinTypes());
        jacksonSubTypesBinder();
        bind(Set.class).annotatedWith(JacksonSubTypes.class).toInstance(Collections.emptySet());
        bind(ClassLoader.class).annotatedWith(GraylogClassLoader.class).toInstance(chainingClassLoader);
        bind(FeatureFlags.class).toInstance(featureFlags);
        bind(InputConfigurationBeanDeserializerModifier.class).toInstance(InputConfigurationBeanDeserializerModifier.withoutConfig());
        bind(ObjectMapper.class).toProvider(ObjectMapperProvider.class).asEagerSingleton();
    }

    protected Multibinder<NamedType> jacksonSubTypesBinder() {
        return Multibinder.newSetBinder(binder(), NamedType.class, JacksonSubTypes.class);
    }



    protected void addPreflightCheck(Class<? extends PreflightCheck> preflightCheck) {
        preflightChecksBinder().addBinding(preflightCheck.getCanonicalName()).to(preflightCheck);
    }

    protected MapBinder<String, PreflightCheck> preflightChecksBinder() {
        return MapBinder.newMapBinder(binder(), String.class, PreflightCheck.class);
    }
}

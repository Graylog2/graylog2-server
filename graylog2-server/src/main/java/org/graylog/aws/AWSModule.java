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
package org.graylog.aws;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.aws.config.AWSConfigurationResource;
import org.graylog.aws.inputs.cloudtrail.CloudTrailCodec;
import org.graylog.aws.inputs.cloudtrail.CloudTrailInput;
import org.graylog.aws.inputs.cloudtrail.CloudTrailTransport;
import org.graylog.aws.migrations.V20200505121200_EncryptAWSSecretKey;
import org.graylog.aws.processors.instancelookup.AWSInstanceNameLookupProcessor;
import org.graylog.aws.processors.instancelookup.InstanceLookupTable;
import org.graylog2.Configuration;
import org.graylog2.plugin.PluginModule;

public class AWSModule extends PluginModule {

    private final Configuration configuration;

    public AWSModule(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    protected void configure() {
        // CloudTrail
        addCodec(CloudTrailCodec.NAME, CloudTrailCodec.class);
        addTransport(CloudTrailTransport.NAME, CloudTrailTransport.class);
        addMessageInput(CloudTrailInput.class);

        bind(ObjectMapper.class).annotatedWith(AWSObjectMapper.class).toInstance(createObjectMapper());

        if (!(configuration.isCloud() || isForwarder())) {
            // Instance name lookup
            addMessageProcessor(AWSInstanceNameLookupProcessor.class, AWSInstanceNameLookupProcessor.Descriptor.class);

            bind(InstanceLookupTable.class).asEagerSingleton();

            addMigration(V20200505121200_EncryptAWSSecretKey.class);
            addRestResource(AWSConfigurationResource.class);
        }
    }

    private ObjectMapper createObjectMapper() {
        return new ObjectMapper()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    /**
     * @return A boolean indicating if the plugin is being loaded within the Graylog Forwarder.
     * The graylog.forwarder system property is set in the startup sequence of the Graylog Cloud Forwarder.
     * <p>
     * The Cloud Forwarder only supports inputs. This allows other bindings to be skipped when this plugin is
     * loaded within the Cloud Forwarder.
     */
    boolean isForwarder() {
        return Boolean.parseBoolean(System.getProperty("graylog.forwarder"));
    }
}

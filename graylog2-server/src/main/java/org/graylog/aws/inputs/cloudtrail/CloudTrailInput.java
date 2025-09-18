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
package org.graylog.aws.inputs.cloudtrail;

import com.amazonaws.regions.Regions;
import com.codahale.metrics.MetricRegistry;
import com.google.inject.assistedinject.Assisted;
import jakarta.inject.Inject;
import org.graylog.aws.AWS;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.DropdownField;
import org.graylog2.plugin.configuration.fields.NumberField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.inputs.CloudCompatible;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;

import java.util.Map;

@CloudCompatible
public class CloudTrailInput extends MessageInput {
    private static final String NAME = "AWS CloudTrail";
    public static final String TYPE = "org.graylog.aws.inputs.cloudtrail.CloudTrailInput";
    public static final String CK_AWS_ACCESS_KEY = "aws_access_key";
    public static final String CK_AWS_SECRET_KEY = "aws_secret_key";
    public static final String CK_CLOUDTRAIL_QUEUE_NAME = "cloudtrail_queue_name";
    public static final String CK_AWS_SQS_REGION = "aws_region";
    public static final String CK_ASSUME_ROLE_ARN = "aws_assume_role_arn";
    private static final Regions DEFAULT_REGION = Regions.US_EAST_1;
    public static final String CK_OVERRIDE_SOURCE = "override_source";
    public static final String CK_POLLING_INTERVAL = "polling_interval";

    @Inject
    public CloudTrailInput(@Assisted Configuration configuration,
                           MetricRegistry metricRegistry,
                           CloudTrailTransport.Factory transport,
                           LocalMetricRegistry localRegistry,
                           CloudTrailCodec.Factory codec,
                           Config config,
                           Descriptor descriptor,
                           ServerStatus serverStatus) {
        super(
                metricRegistry,
                configuration,
                transport.create(configuration),
                localRegistry,
                codec.create(configuration),
                config,
                descriptor,
                serverStatus);
    }

    @FactoryClass
    public interface Factory extends MessageInput.Factory<CloudTrailInput> {
        @Override
        CloudTrailInput create(Configuration configuration);

        @Override
        Config getConfig();

        @Override
        Descriptor getDescriptor();
    }

    public static class Descriptor extends MessageInput.Descriptor {
        public Descriptor() {
            super(NAME, false, "");
        }

        public boolean isForwarderCompatible() {
            return false;
        }

        public boolean isCloudCompatible() {
            return true;
        }
    }

    @ConfigClass
    public static class Config extends MessageInput.Config {
        @Inject
        public Config(CloudTrailTransport.Factory transport, CloudTrailCodec.Factory codec) {
            super(transport.getConfig(), codec.getConfig());
        }

        @Override
        public ConfigurationRequest combinedRequestedConfiguration() {
            final ConfigurationRequest r = super.combinedRequestedConfiguration();

            Map<String, String> regionChoices = AWS.buildRegionChoices();
            r.addField(new TextField(
                    CK_AWS_ACCESS_KEY,
                    "AWS access key",
                    "",
                    "Access key of an AWS user with sufficient permissions.",
                    ConfigurationField.Optional.OPTIONAL
            ));
            r.addField(new TextField(
                    CK_AWS_SECRET_KEY,
                    "AWS secret key",
                    "",
                    "Secret key of an AWS user with sufficient permissions. (See documentation)",
                    ConfigurationField.Optional.OPTIONAL,
                    true
            ));
            r.addField(new DropdownField(
                    CK_AWS_SQS_REGION,
                    "AWS SQS Region",
                    DEFAULT_REGION.getName(),
                    regionChoices,
                    "The AWS region the SQS queue is in.",
                    ConfigurationField.Optional.NOT_OPTIONAL
            ));
            r.addField(new TextField(
                    CK_CLOUDTRAIL_QUEUE_NAME,
                    "CloudTrail SQS Queue name",
                    "",
                    "The name of the SQS Queue created by CloudTrail (cross account access)",
                    ConfigurationField.Optional.OPTIONAL
            ));
            r.addField(new NumberField(
                    CK_POLLING_INTERVAL,
                    "Polling interval",
                    1,
                    "Determines how often Graylog will check for SQS notifications. The smallest allowable interval is 1 minute.",
                    ConfigurationField.Optional.NOT_OPTIONAL));
            r.addField(new TextField(
                    CK_ASSUME_ROLE_ARN,
                    "AWS assume role ARN",
                    "",
                    "The role ARN with required permissions (cross account access)",
                    ConfigurationField.Optional.OPTIONAL
            ));
            r.addField(getOverrideSourceFieldDefinition());

            return r;
        }

        static TextField getOverrideSourceFieldDefinition() {
            return new TextField(
                    CK_OVERRIDE_SOURCE,
                    "Override Source",
                    "",
                    "The source is set to aws-cloudtrail by default. If desired, you may override it with a custom value.",
                    ConfigurationField.Optional.OPTIONAL);
        }
    }

    @Override
    public boolean onlyOnePerCluster() {
        return true;
    }
}

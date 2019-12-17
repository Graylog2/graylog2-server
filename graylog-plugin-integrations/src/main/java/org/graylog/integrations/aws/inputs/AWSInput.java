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
package org.graylog.integrations.aws.inputs;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.assistedinject.Assisted;
import org.graylog.integrations.aws.codecs.AWSCodec;
import org.graylog.integrations.aws.service.AWSService;
import org.graylog.integrations.aws.transports.AWSTransport;
import org.graylog.integrations.aws.transports.KinesisTransport;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.buffers.InputBuffer;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.DropdownField;
import org.graylog2.plugin.configuration.fields.NumberField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.MisfireException;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import software.amazon.awssdk.regions.Region;

import javax.inject.Inject;

/**
 * General AWS input for all types of supported AWS logs.
 */
public class AWSInput extends MessageInput {

    public static final String NAME = "AWS Kinesis/CloudWatch";
    public static final String TYPE = "org.graylog.integrations.aws.inputs.AWSInput";

    public static final String CK_GLOBAL = "global";
    public static final String CK_AWS_REGION = "aws_region";
    public static final String CK_ACCESS_KEY = "aws_access_key";
    public static final String CK_SECRET_KEY = "aws_secret_key";
    public static final String CK_ASSUME_ROLE_ARN = "aws_assume_role_arn";
    public static final String CK_CLOUDWATCH_ENDPOINT = "cloudwatch_endpoint";
    public static final String CK_DYNAMODB_ENDPOINT = "dynamodb_endpoint";
    public static final String CK_IAM_ENDPOINT = "iam_endpoint";
    public static final String CK_KINESIS_ENDPOINT = "kinesis_endpoint";

    @Inject
    public AWSInput(@Assisted Configuration configuration,
                    MetricRegistry metricRegistry,
                    AWSTransport.Factory transport,
                    LocalMetricRegistry localRegistry,
                    AWSCodec.Factory codec,
                    Config config,
                    Descriptor descriptor,
                    ServerStatus serverStatus) {
        super(metricRegistry,
              configuration,
              transport.create(configuration),
              localRegistry,
              codec.create(configuration),
              config,
              descriptor,
              serverStatus);
    }

    @Override
    public void launch(InputBuffer buffer) throws MisfireException {
        super.launch(buffer);
    }

    @Override
    public void stop() {
        super.stop();
    }

    @FactoryClass
    public interface Factory extends MessageInput.Factory<AWSInput> {
        @Override
        AWSInput create(Configuration configuration);

        @Override
        Config getConfig();

        @Override
        Descriptor getDescriptor();
    }

    public static class Descriptor extends MessageInput.Descriptor {
        public Descriptor() {
            super(NAME, false, "");
        }
    }

    @ConfigClass
    public static class Config extends MessageInput.Config {

        private static final String AWS_SDK_ENDPOINT_DESCRIPTION = "Only specify this if you want to override the endpoint, which the AWS SDK communicates with.";

        @Inject
        public Config(AWSTransport.Factory transport, AWSCodec.Factory codec) {
            super(transport.getConfig(), codec.getConfig());
        }

        @Override
        public ConfigurationRequest combinedRequestedConfiguration() {
            ConfigurationRequest request = super.combinedRequestedConfiguration();

            // These config values will be shared amongst many AWS codecs and transports.

            request.addField(new DropdownField(
                    CK_AWS_REGION,
                    "AWS Region",
                    Region.US_EAST_1.id(),
                    AWSService.buildRegionChoices(),
                    "The AWS region the Kinesis stream is running in.",
                    ConfigurationField.Optional.NOT_OPTIONAL));

            request.addField(new TextField(
                    CK_ACCESS_KEY,
                    "AWS access key",
                    "",
                    "Access key of an AWS user with sufficient permissions. (See documentation)",
                    ConfigurationField.Optional.OPTIONAL));

            request.addField(new TextField(
                    CK_SECRET_KEY,
                    "AWS secret key",
                    "",
                    "Secret key of an AWS user with sufficient permissions. (See documentation)",
                    ConfigurationField.Optional.OPTIONAL,
                    TextField.Attribute.IS_PASSWORD));

            request.addField(new TextField(
                    CK_ASSUME_ROLE_ARN,
                    "AWS assume role ARN",
                    "",
                    "Role ARN with required permissions (cross account access)",
                    ConfigurationField.Optional.OPTIONAL));

            request.addField(new TextField(
                    CK_CLOUDWATCH_ENDPOINT,
                    "AWS CloudWatch Override Endpoint",
                    "",
                    AWS_SDK_ENDPOINT_DESCRIPTION,
                    ConfigurationField.Optional.OPTIONAL));

            request.addField(new TextField(
                    CK_DYNAMODB_ENDPOINT,
                    "AWS DynamoDB Override Endpoint",
                    "",
                    AWS_SDK_ENDPOINT_DESCRIPTION,
                    ConfigurationField.Optional.OPTIONAL));

            request.addField(new TextField(
                    CK_IAM_ENDPOINT,
                    "AWS IAM Override Endpoint",
                    "",
                    AWS_SDK_ENDPOINT_DESCRIPTION,
                    ConfigurationField.Optional.OPTIONAL));

            request.addField(new TextField(
                    CK_KINESIS_ENDPOINT,
                    "AWS Kinesis Override Endpoint",
                    "",
                    AWS_SDK_ENDPOINT_DESCRIPTION,
                    ConfigurationField.Optional.OPTIONAL));

            request.addField(new TextField(
                    KinesisTransport.CK_KINESIS_STREAM_NAME,
                    "Kinesis Stream Name",
                    "",
                    "The name of the Kinesis stream that receives your messages. See README for instructions on how to connect messages to a Kinesis Stream.",
                    ConfigurationField.Optional.NOT_OPTIONAL));

            request.addField(new NumberField(
                    KinesisTransport.CK_KINESIS_RECORD_BATCH_SIZE,
                    "Kinesis Record batch size.",
                    KinesisTransport.DEFAULT_BATCH_SIZE,
                    "The number of Kinesis records to fetch at a time. Each record may be up to 1MB in size. The AWS default is 10,000. Enter a smaller value to process smaller chunks at a time.",
                    ConfigurationField.Optional.OPTIONAL,
                    NumberField.Attribute.ONLY_POSITIVE));

            return request;
        }
    }
}

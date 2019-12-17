package org.graylog.integrations.aws;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;
import org.graylog.integrations.aws.resources.requests.AWSRequest;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.core.client.builder.SdkClientBuilder;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClientBuilder;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.IamClientBuilder;
import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.awssdk.services.kinesis.KinesisClientBuilder;

import java.net.URI;

/**
 * Responsible for initializing and building AWS SDK clients. This logic is centralized in one place to ensure consistency amongst the
 * clients and their initialization.
 */
public class AWSClientBuilderUtil {

    // Non-instantiable Util class.
    private AWSClientBuilderUtil() {

    }

    /**
     * Initialize the builder with the appropriate authorization, region, and endpoints.
     *
     * @param builder  Any AWS client builder.
     * @param endpoint See {@link SdkClientBuilder#endpointOverride(java.net.URI)} javadoc.
     * @param region   The region to specify on the client.
     */
    public static void initializeBuilder(AwsClientBuilder builder, String endpoint, Region region, AwsCredentialsProvider credentialsProvider) {
        builder.region(region);
        builder.credentialsProvider(credentialsProvider);

        // The endpoint override explicitly overrides the default URL used for all AWS API communication.
        if (StringUtils.isNotEmpty(endpoint)) {
            builder.endpointOverride(URI.create(endpoint));
        }
    }

    /**
     * Initialize and build the CloudWatch client.
     *
     * @param clientBuilder The builder, which was supplied through dependency injection.
     * @param request       The full AWSRequest.
     * @return A fully built {@link CloudWatchLogsClient}
     */
    public static CloudWatchLogsClient buildClient(CloudWatchLogsClientBuilder clientBuilder, AWSRequest request) {
        Preconditions.checkNotNull(request.region(), "An AWS region is required.");
        AWSClientBuilderUtil.initializeBuilder(clientBuilder,
                                               request.cloudwatchEndpoint(),
                                               Region.of(request.region()),
                                               new AWSAuthProvider(request.region(), request.awsAccessKeyId(),
                                                                   request.awsSecretAccessKey(), request.assumeRoleArn()));

        return clientBuilder.build();
    }

    /**
     * Initialize and build the Kinesis client.
     *
     * @param clientBuilder The builder, which was supplied through dependency injection.
     * @param request       The full AWSRequest.
     * @return A fully built {@link KinesisClient}
     */
    public static KinesisClient buildClient(KinesisClientBuilder clientBuilder, AWSRequest request) {

        AWSClientBuilderUtil.initializeBuilder(clientBuilder,
                                               request.kinesisEndpoint(),
                                               Region.of(request.region()),
                                               new AWSAuthProvider(request.region(), request.awsAccessKeyId(),
                                                                   request.awsSecretAccessKey(), request.assumeRoleArn()));

        return clientBuilder.build();
    }

    /**
     * Initialize and build the IAM client.
     *
     * @param clientBuilder The builder, which was supplied through dependency injection.
     * @param request       The full AWSRequest.
     * @return A fully built {@link IamClient}
     */
    public static IamClient buildClient(IamClientBuilder clientBuilder, AWSRequest request) {

        AWSClientBuilderUtil.initializeBuilder(clientBuilder,
                                               request.iamEndpoint(),
                                               Region.AWS_GLOBAL, // Always specify the global region for the IAM client.
                                               new AWSAuthProvider(request.region(), // The AWSAuthProvider must still use the user-specified region, since a role might need to be assumed in that region.
                                                                   request.awsAccessKeyId(),
                                                                   request.awsSecretAccessKey(), request.assumeRoleArn()));
        return clientBuilder.build();
    }
}
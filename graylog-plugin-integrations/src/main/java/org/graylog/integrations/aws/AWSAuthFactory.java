package org.graylog.integrations.aws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityRequest;

import javax.annotation.Nullable;

import static com.google.common.base.Strings.isNullOrEmpty;

public class AWSAuthFactory {
    private static final Logger LOG = LoggerFactory.getLogger(AWSAuthFactory.class);

    /**
     * Resolves the appropriate AWS authorization provider based on the input.
     *
     * If an {@code accessKey} and {@code secretKey} are provided, they will be used explicitly.
     * If not, the default DefaultCredentialsProvider will be used instead. This will resolve the role/policy
     * using Java props, environment variables, EC2 instance roles etc. See the {@link DefaultCredentialsProvider}
     * Javadoc for more information.
     */
    public static AwsCredentialsProvider create(@Nullable String stsRegion,
                                                @Nullable String accessKey,
                                                @Nullable String secretKey,
                                                @Nullable String assumeRoleArn) {
        AwsCredentialsProvider awsCredentials;
        if (!isNullOrEmpty(accessKey) && !isNullOrEmpty(secretKey)) {
            LOG.debug("Using explicitly provided key and secret.");
            awsCredentials = StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
        } else {
            LOG.debug("Using default authorization provider chain.");
            awsCredentials = DefaultCredentialsProvider.create();
        }

        // Apply the Assume Role ARN Authorization if specified. All AWSCredentialsProviders support this.
        if (!isNullOrEmpty(assumeRoleArn) && !isNullOrEmpty(stsRegion)) {
            LOG.debug("Creating cross account assume role credentials");
            return buildStsCredentialsProvider(awsCredentials, stsRegion, assumeRoleArn, accessKey);
        }

        return awsCredentials;
    }

    /**
     * Build a new AwsCredentialsProvider instance which will assume the indicated role.
     *
     * Note: In order to assume a role, a role must be provided to the AWS STS client a role that has the "sts:AssumeRole"
     * permission, which provides authorization for a role to be assumed.
     */
    private static AwsCredentialsProvider buildStsCredentialsProvider(AwsCredentialsProvider awsCredentials, String stsRegion,
                                                                      String assumeRoleArn, @Nullable String accessKey) {

        StsClient stsClient = StsClient.builder().region(Region.of(stsRegion)).credentialsProvider(awsCredentials).build();

        // The custom roleSessionName is extra metadata, which will be logged in AWS CloudTrail with each request
        // to help with auditing and debugging.
        final String roleSessionName;
        if (accessKey != null) {
            roleSessionName = String.format("ACCESS_KEY_%s@ACCOUNT_%s", accessKey,
                                            stsClient.getCallerIdentity(GetCallerIdentityRequest.builder().build()).account());
        } else {
            roleSessionName = String.format("ACCOUNT_%s",
                                            stsClient.getCallerIdentity(GetCallerIdentityRequest.builder().build()).account());
        }

        LOG.debug("Cross account role session name: " + roleSessionName);
        return StsAssumeRoleCredentialsProvider.builder().refreshRequest(AssumeRoleRequest.builder()
                                                                                          .roleSessionName(roleSessionName)
                                                                                          .roleArn(assumeRoleArn)
                                                                                          .build())
                                               .stsClient(stsClient)
                                               .build();
    }
}
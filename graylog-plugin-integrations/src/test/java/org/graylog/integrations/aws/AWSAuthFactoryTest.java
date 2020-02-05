package org.graylog.integrations.aws;

import org.junit.Assert;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;

public class AWSAuthFactoryTest {

    @Test
    public void testAutomaticAuth() {

        Assert.assertTrue(AWSAuthFactory.create(null, null, null, null) instanceof DefaultCredentialsProvider);
    }

    @Test
    public void testKeySecret() {

        final AwsCredentialsProvider awsCredentialsProvider = AWSAuthFactory.create(null, "key", "secret", null);
        Assert.assertTrue(awsCredentialsProvider instanceof StaticCredentialsProvider);
        Assert.assertEquals("key", awsCredentialsProvider.resolveCredentials().accessKeyId());
        Assert.assertEquals("secret", awsCredentialsProvider.resolveCredentials().secretAccessKey());
    }
}

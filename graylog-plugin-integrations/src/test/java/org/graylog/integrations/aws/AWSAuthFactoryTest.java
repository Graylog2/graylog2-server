package org.graylog.integrations.aws;

import org.junit.Assert;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;

public class AWSAuthFactoryTest {

    @Test
    public void testAutomaticAuth() {

        Assert.assertTrue(AWSAuthFactory.create(null, null, null, null) instanceof DefaultCredentialsProvider);
    }

    @Test
    public void testKeySecret() {

        Assert.assertTrue(AWSAuthFactory.create("key", "secret", null, null) instanceof StaticCredentialsProvider);
    }
}

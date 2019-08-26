package org.graylog.integrations.aws.resources.requests;

/**
 * All AWS API requests should implement this interface.
 * These three fields are needed for all requests.
 */

public interface AWSRequest {

    // Constants are defined here once for all classes.
    String REGION = "region";
    String AWS_ACCESS_KEY_ID = "aws_access_key_id";
    String AWS_SECRET_ACCESS_KEY = "aws_secret_access_key";

    String region();

    String awsAccessKeyId();

    String awsSecretAccessKey();
}
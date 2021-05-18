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

import { AWS_AUTH_TYPES, DEFAULT_KINESIS_LOG_TYPE } from './constants';

export const toAWSRequest = (formData, options) => {
  const {
    awsAuthenticationType,
    awsCloudWatchAssumeARN,
    awsCloudWatchAwsKey,
    awsCloudWatchAwsSecret,
    awsEndpointCloudWatch,
    awsEndpointIAM,
    awsEndpointDynamoDB,
    awsEndpointKinesis,
    key,
    secret,
  } = formData;

  return {
    ...awsAuthenticationType?.value === AWS_AUTH_TYPES.keysecret ? {
      aws_access_key_id: awsCloudWatchAwsKey?.value,
      aws_secret_access_key: awsCloudWatchAwsSecret?.value,
    } : {
      aws_access_key_id: key,
      aws_secret_access_key: secret,
    },
    assume_role_arn: awsCloudWatchAssumeARN?.value,
    cloudwatch_endpoint: awsEndpointCloudWatch?.value,
    dynamodb_endpoint: awsEndpointDynamoDB?.value,
    iam_endpoint: awsEndpointIAM?.value,
    kinesis_endpoint: awsEndpointKinesis?.value,
    ...options,
  };
};

export const toGenericInputCreateRequest = ({
  awsAuthenticationType,
  awsCloudWatchAddFlowLogPrefix = { value: undefined },
  awsCloudWatchAssumeARN = { value: undefined },
  awsCloudWatchAwsKey = { value: undefined },
  awsCloudWatchAwsRegion,
  awsCloudWatchBatchSize,
  awsEndpointCloudWatch = { value: undefined },
  awsCloudWatchGlobalInput = { value: undefined },
  awsCloudWatchKinesisInputType = { value: DEFAULT_KINESIS_LOG_TYPE },
  awsCloudWatchKinesisStream,
  awsCloudWatchName,
  awsCloudWatchThrottleEnabled = { value: undefined },
  awsEndpointDynamoDB = { value: undefined },
  awsEndpointIAM = { value: undefined },
  awsEndpointKinesis = { value: undefined },
  awsCloudWatchAwsSecret,
  key,
  secret,
}) => ({
  type: 'org.graylog.integrations.aws.inputs.AWSInput',
  title: awsCloudWatchName.value,
  global: !!awsCloudWatchGlobalInput.value,
  configuration: {
    ...awsAuthenticationType?.value === AWS_AUTH_TYPES.keysecret ? {
      aws_access_key: awsCloudWatchAwsKey?.value,
      aws_secret_key: awsCloudWatchAwsSecret?.value,
    } : {
      aws_access_key: key,
      aws_secret_key: secret,
    },
    aws_message_type: awsCloudWatchKinesisInputType.value,
    throttling_allowed: !!awsCloudWatchThrottleEnabled.value,
    aws_flow_log_prefix: !!awsCloudWatchAddFlowLogPrefix.value,
    aws_region: awsCloudWatchAwsRegion.value,
    aws_assume_role_arn: awsCloudWatchAssumeARN?.value,
    cloudwatch_endpoint: awsEndpointCloudWatch?.value,
    dynamodb_endpoint: awsEndpointDynamoDB?.value,
    iam_endpoint: awsEndpointIAM?.value,
    kinesis_endpoint: awsEndpointKinesis?.value,
    kinesis_stream_name: awsCloudWatchKinesisStream.value,
    kinesis_record_batch_size: Number(awsCloudWatchBatchSize.value || awsCloudWatchBatchSize.defaultValue),
  },
});

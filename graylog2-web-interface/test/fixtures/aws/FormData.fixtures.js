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
// eslint-disable-next-line import/prefer-default-export
import { AWS_AUTH_TYPES, DEFAULT_KINESIS_LOG_TYPE } from 'integrations/aws/common/constants';

export const exampleFormDataWithKeySecretAuth = {
  awsAuthenticationType: { value: AWS_AUTH_TYPES.keysecret },
  awsCloudWatchAddFlowLogPrefix: { value: true },
  awsCloudWatchAssumeARN: { value: '' },
  awsCloudWatchAwsKey: { value: 'mykey' },
  awsCloudWatchAwsRegion: { value: 'us-east-1' },
  awsCloudWatchBatchSize: { value: 10000 },
  awsEndpointCloudWatch: { value: undefined },
  awsCloudWatchKinesisInputType: { value: DEFAULT_KINESIS_LOG_TYPE },
  awsCloudWatchKinesisStream: { value: 'my-stream' },
  awsCloudWatchName: { value: 'My Input' },
  awsCloudWatchThrottleEnabled: { value: false },
  awsEndpointDynamoDB: { value: undefined },
  awsEndpointIAM: { value: undefined },
  awsEndpointKinesis: { value: undefined },
  awsCloudWatchAwsSecret: { value: 'mysecret' },
};

export const exampleFormDataWithAutomaticAuth = {
  awsAuthenticationType: { value: AWS_AUTH_TYPES.automatic },
  awsCloudWatchAddFlowLogPrefix: { value: true },
  awsCloudWatchAssumeARN: { value: '' },
  awsCloudWatchAwsRegion: { value: 'us-east-1' },
  awsCloudWatchBatchSize: { value: 10000 },
  awsEndpointCloudWatch: { value: undefined },
  awsCloudWatchKinesisInputType: { value: DEFAULT_KINESIS_LOG_TYPE },
  awsCloudWatchKinesisStream: { value: 'my-stream' },
  awsCloudWatchName: { value: 'My Input' },
  awsCloudWatchThrottleEnabled: { value: false },
  awsEndpointDynamoDB: { value: undefined },
  awsEndpointIAM: { value: undefined },
  awsEndpointKinesis: { value: undefined },
  key: 'mykey',
  secret: 'mysecret',
};

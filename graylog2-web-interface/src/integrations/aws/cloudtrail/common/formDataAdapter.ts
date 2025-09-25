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
import type { FormDataType } from 'integrations/types';
import { AWS_AUTH_TYPES } from 'integrations/aws/common/constants';

import type { AWSCloudTrailGenericInputCreateRequest, AWSCloudTrailInputCreateRequest } from '../types';

export const toAWSCloudTrailInputCreateRequest = ({
  awsCloudTrailName,
  awsAuthenticationType,
  awsCloudTrailThrottleEnabled,
  pollingInterval,
  awsAccessKey,
  awsSecretKey,
  awsCloudTrailSqsQueueName,
  awsCloudTrailRegion,
  awsAssumeRoleArn,
  overrideSource,
  key,
  secret
}: FormDataType): AWSCloudTrailInputCreateRequest => ({
  name: awsCloudTrailName?.value,
  ...(awsAuthenticationType?.value === AWS_AUTH_TYPES.keysecret
    ? {
      aws_access_key: awsAccessKey?.value,
      aws_secret_key: awsSecretKey?.value,
    }
    : {
      aws_access_key: key,
      aws_secret_key: secret,
    }),
  polling_interval: pollingInterval?.value,
  enable_throttling: !!awsCloudTrailThrottleEnabled?.value,
  cloudtrail_queue_name: awsCloudTrailSqsQueueName?.value,
  aws_region: awsCloudTrailRegion?.value,
  assume_role_arn: awsAssumeRoleArn?.value,
  override_source: overrideSource?.value,
});

export const toGenericInputCreateRequest = ({
  awsCloudTrailName,
  awsAuthenticationType,
  awsCloudTrailThrottleEnabled,
  awsAccessKey,
  awsSecretKey,
  pollingInterval,
  awsCloudTrailSqsQueueName,
  awsCloudTrailRegion,
  awsAssumeRoleArn,
  key,
  secret,
  overrideSource,
}: FormDataType): AWSCloudTrailGenericInputCreateRequest => ({
  type: 'org.graylog.aws.inputs.cloudtrail.CloudTrailInput',
  title: awsCloudTrailName?.value,
  global: false,
  configuration: {
    ...(awsAuthenticationType?.value === AWS_AUTH_TYPES.keysecret
      ? {
        aws_access_key: awsAccessKey?.value,
        aws_secret_key: awsSecretKey?.value,
      }
      : {
        aws_access_key: key,
        aws_secret_key: secret,
      }),
    polling_interval: pollingInterval?.value,
    throttling_allowed: !!awsCloudTrailThrottleEnabled?.value,
    cloudtrail_queue_name: awsCloudTrailSqsQueueName?.value,
    aws_region: awsCloudTrailRegion?.value,
    assume_role_arn: awsAssumeRoleArn?.value,
    override_source: overrideSource?.value,
  },
});

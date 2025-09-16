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

import type { AWSCloudTrailGenericInputCreateRequest, AWSCloudTrailInputCreateRequest } from '../types';

export const toAWSCloudTrailInputCreateRequest = ({
  awsCloudTrailName,
  awsCloudTrailStoreFullMessage,
  awsCloudTrailThrottleEnabled,
  pollingInterval,
  awsCloudTrailAccessKeyId,
  awsCloudTrailSecretKey,
  awsCloudTrailSqsQueueName,
  awsCloudTrailRegion,
  assumeRoleArn,
}: FormDataType): AWSCloudTrailInputCreateRequest => ({
  name: awsCloudTrailName.value,
  polling_interval: pollingInterval.value,
  store_full_message: !!awsCloudTrailStoreFullMessage?.value,
  enable_throttling: !!awsCloudTrailThrottleEnabled?.value,
  cloudtrail_queue_name: awsCloudTrailSqsQueueName.value,
  aws_access_key: awsCloudTrailAccessKeyId.value,
  aws_secret_key: awsCloudTrailSecretKey.value,
  aws_region: awsCloudTrailRegion.value,
  assume_role_arn: assumeRoleArn?.value,
});

export const toGenericInputCreateRequest = ({
  awsCloudTrailName,
  awsCloudTrailStoreFullMessage,
  awsCloudTrailThrottleEnabled,
  awsCloudTrailAccessKeyId,
  awsCloudTrailSecretKey,
  pollingInterval,
  awsCloudTrailSqsQueueName,
  awsCloudTrailRegion,
  assumeRoleArn,
}: FormDataType): AWSCloudTrailGenericInputCreateRequest => ({
  type: 'org.graylog.aws.inputs.cloudtrail.CloudTrailInput',
  title: awsCloudTrailName.value,
  global: false,
  configuration: {
    polling_interval: pollingInterval.value,
    store_full_message: !!awsCloudTrailStoreFullMessage?.value,
    throttling_allowed: !!awsCloudTrailThrottleEnabled?.value,
    cloudtrail_queue_name: awsCloudTrailSqsQueueName.value,
    aws_access_key: awsCloudTrailAccessKeyId.value,
    aws_secret_key: awsCloudTrailSecretKey.value,
    aws_region: awsCloudTrailRegion.value,
    assume_role_arn: assumeRoleArn?.value,
  },
});

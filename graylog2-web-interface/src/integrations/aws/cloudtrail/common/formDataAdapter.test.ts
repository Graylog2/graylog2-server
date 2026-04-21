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
import { AWS_AUTH_TYPES } from 'integrations/aws/common/constants';

import { toAWSCloudTrailInputCreateRequest, toGenericInputCreateRequest } from './formDataAdapter';

const baseFormData = {
  awsCloudTrailName: { value: 'Test CloudTrail Input' },
  awsAuthenticationType: { value: AWS_AUTH_TYPES.keysecret },
  awsCloudTrailThrottleEnabled: { value: true },
  pollingInterval: { value: 5 },
  awsAccessKey: { value: 'test-access-key' },
  awsSecretKey: { value: 'test-secret-key' },
  awsCloudTrailSqsQueueName: { value: 'test-queue' },
  awsCloudTrailSqsRegion: { value: 'us-east-1' },
  awsCloudTrailS3Region: { value: 'us-east-2' },
  awsAssumeRoleARN: { value: 'arn:aws:iam::123456789012:role/test-role' },
  overrideSource: { value: '' },
  sqsMessageBatchSize: { value: 10 },
  includeFullMessageJson: { value: false },
};

describe('CloudTrail formDataAdapter', () => {
  describe('toAWSCloudTrailInputCreateRequest', () => {
    it('includes assume_role_arn in the request', () => {
      const request = toAWSCloudTrailInputCreateRequest(baseFormData);

      expect(request.assume_role_arn).toBe('arn:aws:iam::123456789012:role/test-role');
    });

    it('sets assume_role_arn to undefined when not provided', () => {
      const formData = { ...baseFormData, awsAssumeRoleARN: { value: undefined } };
      const request = toAWSCloudTrailInputCreateRequest(formData);

      expect(request.assume_role_arn).toBeUndefined();
    });
  });

  describe('toGenericInputCreateRequest', () => {
    it('includes assume_role_arn in configuration', () => {
      const request = toGenericInputCreateRequest(baseFormData);

      expect(request.configuration.assume_role_arn).toBe('arn:aws:iam::123456789012:role/test-role');
    });

    it('sets assume_role_arn to undefined when not provided', () => {
      const formData = { ...baseFormData, awsAssumeRoleARN: { value: undefined } };
      const request = toGenericInputCreateRequest(formData);

      expect(request.configuration.assume_role_arn).toBeUndefined();
    });
  });
});

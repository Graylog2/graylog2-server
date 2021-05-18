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

import { exampleFormDataWithAutomaticAuth, exampleFormDataWithKeySecretAuth } from 'aws/FormData.fixtures';

import { toAWSRequest, toGenericInputCreateRequest } from './formDataAdapter';
import { AWS_AUTH_TYPES } from './constants';

describe('formDataAdapter', () => {
  const testGenericInputCreateRequest = (formData) => {
    let awsAccessKey = 'key';
    let awsAccessSecret = 'secret';

    if (formData.awsAuthenticationType?.value === AWS_AUTH_TYPES.keysecret) {
      awsAccessKey = 'awsCloudWatchAwsKey';
      awsAccessSecret = 'awsCloudWatchAwsSecret';
    }

    // Mapping keys taken from /api/system/inputs/types/org.graylog.integrations.aws.inputs.AWSInput
    const mappings = {
      aws_access_key: awsAccessKey,
      aws_assume_role_arn: 'awsCloudWatchAssumeARN',
      aws_flow_log_prefix: 'awsCloudWatchAddFlowLogPrefix',
      aws_message_type: 'awsCloudWatchKinesisInputType',
      aws_region: 'awsCloudWatchAwsRegion',
      aws_secret_key: awsAccessSecret,
      cloudwatch_endpoint: 'awsEndpointCloudWatch',
      dynamodb_endpoint: 'awsEndpointDynamoDB',
      iam_endpoint: 'awsEndpointIAM',
      kinesis_endpoint: 'awsEndpointKinesis',
      kinesis_record_batch_size: 'awsCloudWatchBatchSize',
      kinesis_stream_name: 'awsCloudWatchKinesisStream',
      throttling_allowed: 'awsCloudWatchThrottleEnabled',
    };

    const request = toGenericInputCreateRequest(formData);

    expect(request.type).toBe('org.graylog.integrations.aws.inputs.AWSInput');
    expect(request.title).toEqual(formData.awsCloudWatchName.value);
    expect(request.global).toEqual(formData.awsCloudWatchGlobalInput.value);

    const { configuration } = request;

    expect(Object.keys(configuration).sort()).toEqual(Object.keys(mappings).sort());

    Object.entries(configuration).forEach(([key, value]) => {
      const formDataValue = (mappings[key] === 'key' || mappings[key] === 'secret'
        ? formData[mappings[key]]
        : formData[mappings[key]].value);

      expect(value).toEqual(formDataValue);
    });

    return request;
  };

  const testAWSRequest = (formData, options = {}) => {
    let awsAccessKey = 'key';
    let awsAccessSecret = 'secret';

    if (formData.awsAuthenticationType?.value === AWS_AUTH_TYPES.keysecret) {
      awsAccessKey = 'awsCloudWatchAwsKey';
      awsAccessSecret = 'awsCloudWatchAwsSecret';
    }

    const mappings = {
      aws_access_key_id: awsAccessKey,
      aws_secret_access_key: awsAccessSecret,
      assume_role_arn: 'awsCloudWatchAssumeARN',
      cloudwatch_endpoint: 'awsEndpointCloudWatch',
      dynamodb_endpoint: 'awsEndpointDynamoDB',
      iam_endpoint: 'awsEndpointIAM',
      kinesis_endpoint: 'awsEndpointKinesis',
    };

    const request = toAWSRequest(formData, options);

    expect(Object.keys(request).sort()).toEqual(Object.keys({ ...mappings, ...options }).sort());

    const optionsMap = new Map(Object.entries(options));

    Object.entries(request).forEach(([key, value]) => {
      if (optionsMap.has(key)) {
        return;
      }

      const formDataValue = (mappings[key] === 'key' || mappings[key] === 'secret'
        ? formData[mappings[key]]
        : formData[mappings[key]].value);

      expect(value).toEqual(formDataValue);
    });

    return request;
  };

  it('adapts formData into an AWS request with key & secret', () => {
    testAWSRequest({
      awsAuthenticationType: { value: AWS_AUTH_TYPES.keysecret },
      awsCloudWatchAssumeARN: { value: '' },
      awsCloudWatchAwsKey: { value: 'mykey' },
      awsEndpointCloudWatch: { value: undefined },
      awsEndpointDynamoDB: { value: undefined },
      awsEndpointIAM: { value: undefined },
      awsEndpointKinesis: { value: undefined },
      awsCloudWatchAwsSecret: { value: 'mysecret' },
    });
  });

  it('adapts formData into an AWS request with automatic auth', () => {
    testAWSRequest({
      awsAuthenticationType: { value: AWS_AUTH_TYPES.automatic },
      awsCloudWatchAssumeARN: { value: '' },
      key: 'mykey',
      awsEndpointCloudWatch: { value: undefined },
      awsEndpointDynamoDB: { value: undefined },
      awsEndpointIAM: { value: undefined },
      awsEndpointKinesis: { value: undefined },
      secret: 'mysecret',
    });
  });

  it('adapts formData into an AWS request with additional options', () => {
    const options = {
      name: 'foobar',
      global: true,
    };

    const request = testAWSRequest({
      awsAuthenticationType: { value: AWS_AUTH_TYPES.keysecret },
      awsCloudWatchAssumeARN: { value: '' },
      awsCloudWatchAwsKey: { value: 'mykey' },
      awsEndpointCloudWatch: { value: undefined },
      awsEndpointDynamoDB: { value: undefined },
      awsEndpointIAM: { value: undefined },
      awsEndpointKinesis: { value: undefined },
      awsCloudWatchAwsSecret: { value: 'mysecret' },
    }, options);

    expect(request).toMatchObject(options);
  });

  it('adapts formData into an InputCreateRequest with key & secret', () => {
    testGenericInputCreateRequest(exampleFormDataWithKeySecretAuth);
  });

  it('adapts formData into an InputCreateRequest with automatic auth', () => {
    testGenericInputCreateRequest(exampleFormDataWithAutomaticAuth);
  });
});

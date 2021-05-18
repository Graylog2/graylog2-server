// eslint-disable-next-line import/prefer-default-export
import { AWS_AUTH_TYPES, DEFAULT_KINESIS_LOG_TYPE } from 'aws/common/constants';

export const exampleFormDataWithKeySecretAuth = {
  awsAuthenticationType: { value: AWS_AUTH_TYPES.keysecret },
  awsCloudWatchAddFlowLogPrefix: { value: true },
  awsCloudWatchAssumeARN: { value: '' },
  awsCloudWatchAwsKey: { value: 'mykey' },
  awsCloudWatchAwsRegion: { value: 'us-east-1' },
  awsCloudWatchBatchSize: { value: 10000 },
  awsEndpointCloudWatch: { value: undefined },
  awsCloudWatchGlobalInput: { value: false },
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
  awsCloudWatchGlobalInput: { value: false },
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

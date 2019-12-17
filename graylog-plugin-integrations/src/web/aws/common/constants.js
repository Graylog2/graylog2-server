const KINESIS_LOG_TYPES = [
  { value: 'KINESIS_CLOUDWATCH_FLOW_LOGS', label: 'Kinesis CloudWatch Flow Logs' },
  { value: 'KINESIS_CLOUDWATCH_RAW', label: 'Kinesis CloudWatch Raw' },
  { value: 'KINESIS_RAW', label: 'Kinesis Raw' },
];

const DEFAULT_KINESIS_LOG_TYPE = 'KINESIS_CLOUDWATCH_FLOW_LOGS';

const AWS_AUTH_TYPES = {
  automatic: 'Automatic',
  keysecret: 'Key & Secret',
};

export {
  AWS_AUTH_TYPES,
  KINESIS_LOG_TYPES,
  DEFAULT_KINESIS_LOG_TYPE,
};

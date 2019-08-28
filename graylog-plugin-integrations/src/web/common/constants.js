const KINESIS_LOG_TYPES = [
  { value: 'KINESIS_FLOW_LOGS', label: 'Kinesis Flow Logs' },
  { value: 'KINESIS_RAW', label: 'Kinesis Raw' },
];

const DEFAULT_KINESIS_LOG_TYPE = 'KINESIS_RAW';

export {
  KINESIS_LOG_TYPES,
  DEFAULT_KINESIS_LOG_TYPE,
};

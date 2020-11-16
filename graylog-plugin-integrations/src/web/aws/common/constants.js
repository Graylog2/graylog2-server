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

import { fromPairs } from 'lodash';

const ES_FIELDS = [
  '_id',
  '_ttl',
  '_source',
  '_all',
  '_index',
  '_type',
  '_score',
];

const GRAYLOG_FIELDS = [
  'gl2_source_node',
  'gl2_source_input',

  'gl2_source_collector',
  'gl2_source_collector_input',
  'gl2_remote_ip',
  'gl2_remote_port',
  'gl2_remote_hostname',
];

const STREAMS_FIELD = 'streams';
const FULL_MESSAGE_FIELD = 'full_message';

const FILTERED_FIELDS = [].concat(ES_FIELDS, GRAYLOG_FIELDS, STREAMS_FIELD, FULL_MESSAGE_FIELD);

export default (message) => {
  return fromPairs(Object.entries(message).filter(([k, _]) => !FILTERED_FIELDS.includes(k)));
};
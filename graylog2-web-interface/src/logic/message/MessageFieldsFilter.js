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
const MessageFieldsFilter = {
  FILTERED_FIELDS: [
    // ElasticSearch fields.
    '_id',
    '_ttl',
    '_source',
    '_all',
    '_index',
    '_type',
    '_score',

    // Our reserved fields.
    'gl2_accounted_message_size',
    'gl2_message_id',
    'gl2_source_node',
    'gl2_source_input',
    'gl2_source_collector',
    'gl2_source_collector_input',
    'gl2_remote_ip',
    'gl2_remote_port',
    'gl2_remote_hostname',
    'streams',
    // TODO Due to be removed in Graylog 3.x
    'gl2_source_radio',
    'gl2_source_radio_input',
  ],
  filterFields(fields) {
    const result = {};

    Object.keys(fields).forEach((field) => {
      if (this.FILTERED_FIELDS.indexOf(field) < 0) {
        result[field] = fields[field];
      }
    });

    return result;
  },
};

export default MessageFieldsFilter;

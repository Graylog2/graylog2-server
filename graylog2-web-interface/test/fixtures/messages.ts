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

// eslint-disable-next-line import/prefer-default-export
export const simpleMessage = {
  decoration_stats: null,
  fields: { took_ms: 62, source: 'example.org' },
  formatted_fields: { took_ms: 62, source: 'example.org' },
  highlight_ranges: {},
  id: 'message-id',
  index: 'grayog_0',
};

export const message = {
  id: '20f683d2-a874-11e9-8a11-0242ac130004',
  timestamp: '2019-07-17T11:20:33.000Z',
  filtered_fields: {
    level: 6,
    source: 'babbage',
    message: 'babbage 30ac6e35e442[27354]: [2019-07-17T09:20:33,415][WARN ][o.e.d.c.ParseField       ] [Mc1oQWu] Deprecated field [split_on_whitespace] used, replaced by [This setting is ignored, the parser always splits on operator]',
    hostname: 'message-hostname',
    facility: 'user-level',
    timestamp: '2019-07-17T11:20:33.000Z',
  },
  formatted_fields: {
    level: 6,
    source: 'babbage',
    message: 'babbage 30ac6e35e442[27354]: [2019-07-17T09:20:33,415][WARN ][o.e.d.c.ParseField       ] [Mc1oQWu] Deprecated field [split_on_whitespace] used, replaced by [This setting is ignored, the parser always splits on operator]',
    hostname: 'message-hostname',
    facility: 'user-level',
    timestamp: '2019-07-17T11:20:33.000Z',
  },
  fields: {
    level: 6,
    gl2_remote_ip: '192.168.1.47',
    gl2_remote_port: 35024,
    streams: ['000000000000000000000001'],
    gl2_message_id: '01DFZKQF4D3642JY91FM6Z1WQG',
    source: 'babbage',
    message: 'babbage 30ac6e35e442[27354]: [2019-07-17T09:20:33,415][WARN ][o.e.d.c.ParseField       ] [Mc1oQWu] Deprecated field [split_on_whitespace] used, replaced by [This setting is ignored, the parser always splits on operator]',
    gl2_source_input: '5c26a37b3885e50480aa12a2',
    hostname: 'message-hostname',
    gl2_source_node: '4c0cbe7b-c51a-4617-bb50-ea01fe6dbfd0',
    _id: '20f683d2-a874-11e9-8a11-0242ac130004',
    facility: 'user-level',
    timestamp: '2019-07-17T11:20:33.000Z',
  },
  index: 'graylog_5',
  source_node_id: '4c0cbe7b-c51a-4617-bb50-ea01fe6dbfd0',
  source_input_id: '5c26a37b3885e50480aa12a2',
  stream_ids: [],
  highlight_ranges: {},
};

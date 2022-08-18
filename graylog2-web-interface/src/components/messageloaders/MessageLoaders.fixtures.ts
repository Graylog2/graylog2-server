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

import { Map } from 'immutable';

import type { Input } from 'components/messageloaders/Types';

export const input: Input = {
  title: 'syslog udp',
  global: true,
  name: 'Syslog UDP',
  content_pack: null,
  created_at: '2019-07-15T07:25:12.397Z',
  type: 'org.graylog2.inputs.syslog.udp.SyslogUDPInput',
  creator_user_id: 'admin',
  attributes: {
    expand_structured_data: false,
    recv_buffer_size: 262144,
    port: 12514,
    number_worker_threads: 8,
    override_source: null,
    force_rdns: false,
    allow_override_date: true,
    bind_address: '0.0.0.0',
    store_full_message: false,
  },
  static_fields: {},
  node: '4c0cbe7b-c51a-4617-bb50-ea01fe6dbfd0',
  id: '5c26a37b3885e50480aa12a2',
};

export const inputs = Map<string, Input>([[input.id, input]]);

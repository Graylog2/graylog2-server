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
import type {
  LookupTable,
  LookupTableCache,
  LookupTableAdapter,
} from 'logic/lookup-tables/types';

export const createLookupTable = (index = 1, overrides = {}): LookupTable => ({
  id: `lookup-table-id-${index}`,
  title: `Lookup Table Title ${index}`,
  description: `Description lookup-table-${index}`,
  name: `lookup-table-name-${index}`,
  content_pack: null,
  _scope: 'DEFAULT',
  cache_id: 'cache-id',
  data_adapter_id: 'data-adapter-id',
  default_multi_value: '',
  default_multi_value_type: 'NULL',
  default_single_value: '',
  default_single_value_type: 'NULL',
  ...overrides,
});

export const createLookupTableCache = (index = 1, overrides = {}): LookupTableCache => ({
  id: `lookup-table-id-${index}`,
  title: `Lookup Table Title ${index}`,
  description: `Description lookup-table-${index}`,
  name: `lookup-table-name-${index}`,
  content_pack: null,
  _scope: 'DEFAULT',
  config: {
    type: 'guava_cache',
    max_size: 1000,
    expire_after_access: 60,
    expire_after_access_unit: 'SECONDS',
    expire_after_write: 0,
    expire_after_write_unit: 'MILLISECONDS',
  },
  ...overrides,
});

export const createLookupTableAdapter = (index = 1, overrides = {}): LookupTableAdapter => ({
  id: `lookup-table-id-${index}`,
  title: `Lookup Table Title ${index}`,
  description: `Description lookup-table-${index}`,
  name: `lookup-table-name-${index}`,
  content_pack: null,
  _scope: 'DEFAULT',
  config: {
    type: 'csvfile',
    path: '/data/node-01/illuminate/csv/ciscoasa/data/cisco_asa_event_codes.csv',
    override_type: 'mongo',
    separator: ',',
    quotechar: '"',
    key_column: 'cisco_event_code',
    value_column: 'gim_event_type_code',
    check_interval: 60,
    case_insensitive_lookup: false,
  },
  ...overrides,
});

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
import type { LookupTable, LookupTableCache, LookupTableAdapter } from 'logic/lookup-tables/types';

const iterable = Object.keys([...new Array(10)]);

export const LOOKUP_TABLES: Array<LookupTable> = iterable.map((item: string) => ({
  id: `${item}-table-id`,
  _scope: 'DEFAULT',
  title: `${item} table title`,
  description: `${item} table description`,
  name: `${item} table name`,
  cache_id: `${item}-cache-id`,
  data_adapter_id: `${item}-data-adapter-id`,
  content_pack: null,
  default_single_value: '',
  default_single_value_type: 'NULL',
  default_multi_value: '',
  default_multi_value_type: 'NULL',
}));

export const CACHES: Array<LookupTableCache> = iterable.map((item: string) => ({
  config: {
    type: 'none',
  },
  id: `${item}-cache-id`,
  _scope: 'DEFAULT',
  title: `${item} cache title`,
  description: `${item} cache description`,
  name: `${item} cache name`,
  content_pack: null,
}));

export const DATA_ADAPTERS: Array<LookupTableAdapter> = iterable.map((item: string) => ({
  id: `${item}-data-adapter-id`,
  _scope: 'DEFAULT',
  title: `${item} adapter title`,
  description: `${item} adapter description`,
  name: `${item} adapter name`,
  custom_error_ttl_enabled: false,
  custom_error_ttl: null,
  custom_error_ttl_unit: null,
  content_pack: null,
  config: {
    type: 'torexitnode',
  },
}));

export const CACHES_MAP = Object.fromEntries(CACHES.map((cache) => [cache.id, cache]));
export const ADAPTERS_MAP = Object.fromEntries(DATA_ADAPTERS.map((adapter) => [adapter.id, adapter]));

export const ERROR_STATE = {
  tables: Object.fromEntries(
    LOOKUP_TABLES.map(({ name }: { name: string }, i: number) => [name, i === 1 ? 'Lookup table test error' : null]),
  ),
  data_adapters: Object.fromEntries(
    Object.values(ADAPTERS_MAP).map(({ name }: { name: string }, i: number) => [
      name,
      i === 1 ? 'Adapter test error' : null,
    ]),
  ),
  caches: Object.fromEntries(
    Object.values(CACHES_MAP).map(({ name }: { name: string }, i: number) => [
      name,
      i === 1 ? 'Cache test error' : null,
    ]),
  ),
};

export const ERRORS_CONTEXT_VALUE = {
  errors: {
    lutErrors: { ...ERROR_STATE.tables },
    cacheErrors: { ...ERROR_STATE.caches },
    adapterErrors: { ...ERROR_STATE.data_adapters },
  },
  setErrors: () => {},
};

export const UNSUPPORTED_PREVIEW = { supported: false, total: 0, results: {} };
export const SUPPORTED_PREVIEW = {
  supported: true,
  total: 12,
  results: {
    100: 'Continue',
    101: 'Switching Protocols',
    200: 'OK',
    102: 'Processing',
    201: 'Created',
    103: 'Early Hints',
    202: 'Accepted',
    203: 'Non-Authoritative Information',
    204: 'No Content',
    205: 'Reset Content',
    206: 'Partial Content',
    207: 'Multi-Status',
  },
};

export const TEST_KEY_RESULT = {
  single_value: 'Non-Authoritative Information',
  multi_value: {
    value: 'Non-Authoritative Information',
  },
  string_list_value: null,
  has_error: false,
  ttl: 9,
};

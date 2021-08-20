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

type LookupCacheConfiguration = {
  type: string,
}

export type LookupTableCache = {
  name: string,
  description: string | null,
  id: string | null,
  title: string,
  content_pack: string | null,
  config: LookupCacheConfiguration,
}

type LookupDataAdapterConfiguration = {
  type: string,
}

export type LookupTableAdapter = {
  custom_error_ttl: number | null,
  name: string,
  description: string,
  id: string | null,
  title: string,
  config: LookupDataAdapterConfiguration,
  content_pack: string | null,
  custom_error_ttl_unit: 'NANOSECONDS' | 'MICROSECONDS' | 'MILLISECONDS' | 'SECONDS' | 'MINUTES' | 'HOURS' | 'DAYS' | null,
  custom_error_ttl_enabled (boolean, optional)
}

export type LookupTable = {
  cache_id: string;
  default_multi_value_type: string | number | object | boolean | null;
  name: string;
  description: string | null;
  id: string | null;
  title: string;
  default_single_value_type: string | number | object | boolean | null;
  content_pack: string | null;
  data_adapter_id: string;
  default_multi_value: string | null;
  default_single_value: string | null;
}

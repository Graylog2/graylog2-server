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
export type GenericEntityType = {
  id?: string | null,
  title: string,
  description?: string | null,
  name: string,
  content_pack?: string | null,
  _scope?: string,
};

export type LookupTableCacheConfig = {
  type?: string,
  max_size?: number,
  expire_after_access?: number,
  expire_after_access_unit?: 'NANOSECONDS' | 'MICROSECONDS' | 'MILLISECONDS' | 'SECONDS' | 'MINUTES' | 'HOURS' | 'DAYS' | null,
  expire_after_write?: number,
  expire_after_write_unit?: 'NANOSECONDS' | 'MICROSECONDS' | 'MILLISECONDS' | 'SECONDS' | 'MINUTES' | 'HOURS' | 'DAYS' | null,
};

export type LookupTableCache = GenericEntityType & {
  config?: LookupTableCacheConfig
}

export type LookupTableDataAdapterConfig = {
  type?: string,
  path?: string,
  override_type?: string,
  separator?: string,
  quotechar?: string,
  key_column?: string,
  value_column?: string,
  check_interval?: number,
  case_insensitive_lookup?: boolean,
};

export type LookupTableAdapter = GenericEntityType & {
  config?: LookupTableDataAdapterConfig,
  custom_error_ttl?: number | null,
  custom_error_ttl_unit?: 'NANOSECONDS' | 'MICROSECONDS' | 'MILLISECONDS' | 'SECONDS' | 'MINUTES' | 'HOURS' | 'DAYS' | null,
  custom_error_ttl_enabled?: boolean,
}

export type LookupTable = GenericEntityType & {
  cache_id?: string,
  default_multi_value_type?: 'STRING' | 'NUMBER' | 'OBJECT' | 'BOOLEAN' | 'NULL',
  default_single_value_type?: 'STRING' | 'NUMBER' | 'OBJECT' | 'BOOLEAN' | 'NULL',
  data_adapter_id?: string,
  default_multi_value?: string | null,
  default_single_value?: string | null,
}

export type validationErrorsType = { name?: string, message?: string };

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
import type { LookupTableDataAdapterConfig } from 'logic/lookup-tables/types';

export type DVSHTTPAdapterConfig = LookupTableDataAdapterConfig & {
  url?: string,
  refresh_interval?: number,
  line_separator?: string,
  ignorechar?: string,
  check_presence_only?: boolean
};

export type DnsAdapterConfig = LookupTableDataAdapterConfig & {
  lookup_type?: string,
  server_ips?: string,
  request_timeout?: number,
  cache_ttl_override?: number,
  cache_ttl_override_unit?: 'NANOSECONDS' | 'MICROSECONDS' | 'MILLISECONDS' | 'SECONDS' | 'MINUTES' | 'HOURS' | 'DAYS' | null,
  cache_ttl_override_enabled?: boolean,
};

export type HTTPJSONPathAdapterConfig = LookupTableDataAdapterConfig & {
  url?: string,
  single_value_jsonpath?: string,
  multi_value_jsonpath?: string,
  user_agent?: string,
  headers?: { [key: string]: string },
};

export type ConfigFieldSetType = DVSHTTPAdapterConfig | DnsAdapterConfig | HTTPJSONPathAdapterConfig;

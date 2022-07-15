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

type Metadata = {
  scope: string,
  revision: number,
  created_at: string,
  updated_at: string,
}

type GenericEntityType = {
  id?: string | null,
  title: string,
  description?: string | null,
  name: string,
  content_pack?: string | null,
  _metadata?: Metadata | null,
};

export type LookupTableCache = GenericEntityType & {
  config?: { type: string },
}

export type LookupTableAdapter = GenericEntityType & {
  custom_error_ttl?: number | null,
  config?: { type: string },
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

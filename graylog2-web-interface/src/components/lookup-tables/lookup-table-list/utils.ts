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
import type { PaginatedResponseType } from 'stores/PaginationTypes';

import { attributes } from './constants';

type DeserializeLookupTablesArgs = PaginatedResponseType & {
  lookup_tables: Array<LookupTable>;
  caches: { [key: string]: LookupTableCache };
  data_adapters: { [key: string]: LookupTableAdapter };
};

export default function deserializeLookupTables({
  query,
  total,
  page,
  per_page,
  count,
  lookup_tables,
  caches,
  data_adapters,
}: DeserializeLookupTablesArgs) {
  return {
    attributes,
    list: lookup_tables.map((lut: LookupTable) => ({ ...lut, id: lut.id })) ?? [],
    pagination: { total, page, per_page, count, query },
    meta: {
      caches,
      adapters: data_adapters,
    },
  };
}

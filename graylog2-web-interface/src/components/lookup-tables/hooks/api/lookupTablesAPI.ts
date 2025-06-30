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
import { LookupTablesActions } from 'stores/lookup-tables/LookupTablesStore';
import { LookupTableCachesActions } from 'stores/lookup-tables/LookupTableCachesStore';
import type { SearchParams } from 'stores/PaginationTypes';
import deserializeLookupTables from 'components/lookup-tables/lookup-table/utils';
import deserializeCaches from 'components/lookup-tables/cache-list/utils';

export const fetchErrors = async ({
  lutNames = undefined,
  cacheNames = undefined,
  adapterNames = undefined,
}: {
  lutNames?: Array<string>;
  cacheNames?: Array<string>;
  adapterNames?: Array<string>;
}) => LookupTablesActions.getErrors(lutNames, cacheNames, adapterNames);

export const fetchPaginatedLookupTables = async (searchParams: SearchParams) => {
  const { page, pageSize, query } = searchParams;

  return LookupTablesActions.searchPaginated(page, pageSize, query).then(deserializeLookupTables);
};

export const fetchPaginatedCaches = async (searchParams: SearchParams) => {
  const { page, pageSize, query } = searchParams;

  return LookupTableCachesActions.searchPaginated(page, pageSize, query).then((resp: any) => deserializeCaches(resp));
};

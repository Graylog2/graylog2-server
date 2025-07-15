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
import { LookupTableDataAdaptersActions, LookupTableDataAdaptersStore } from 'stores/lookup-tables/LookupTableDataAdaptersStore';
import { LookupTableCachesActions, LookupTableCachesStore } from 'stores/lookup-tables/LookupTableCachesStore';
import deserializeLookupTables from 'components/lookup-tables/lookup-table-list/utils';
import deserializeCaches from 'components/lookup-tables/cache-list/utils';
import deserializeDataAdapters from 'components/lookup-tables/adapter-list/utils';
import type { SearchParams } from 'stores/PaginationTypes';
import type { LookupTableCache } from 'logic/lookup-tables/types';

export const deleteLookupTable = async (tableId: string) => LookupTablesActions.delete(tableId);

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

export const fetchCacheTypes = async () => {
  await LookupTableCachesActions.getTypes();
  const state = LookupTableCachesStore.getInitialState();

  return state.types;
};

export const validateCache = async (cache) => {
  await LookupTableCachesActions.validate(cache);
  const state = LookupTableCachesStore.getInitialState();

  return state.validationErrors;
};

export const createCache = async (payload: LookupTableCache) => LookupTableCachesActions.create(payload);

export const updateCache = async (payload: LookupTableCache) => LookupTableCachesActions.update(payload);

export const deleteCache = async (cacheId: string) => LookupTableCachesActions.delete(cacheId);

export const fetchPaginatedDataAdapters = async (searchParams: SearchParams) => {
  const { page, pageSize, query } = searchParams;

  return LookupTableDataAdaptersActions.searchPaginated(page, pageSize, query).then(deserializeDataAdapters);
};

export const fetchDataAdapterTypes = async () => {
  await LookupTableDataAdaptersActions.getTypes();
  const state = LookupTableDataAdaptersStore.getInitialState();

  return state.types;
};

export const createDataAdapter = async (payload: LookupTableCache) => LookupTableDataAdaptersActions.create(payload);

export const updateDataAdapter = async (payload: LookupTableCache) => LookupTableDataAdaptersActions.update(payload);

export const validateDataAdapter = async (adapter) => LookupTableDataAdaptersActions.validate(adapter);

export const deleteDataAdapter = async (adapterId: string) => LookupTableDataAdaptersActions.delete(adapterId);

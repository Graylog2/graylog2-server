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
import { ClusterLookupTable, SystemLookup } from '@graylog/server-api';

import { qualifyUrl } from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import PaginationURL from 'util/PaginationURL';
import deserializeLookupTables from 'components/lookup-tables/lookup-table-list/utils';
import deserializeCaches from 'components/lookup-tables/cache-list/utils';
import deserializeDataAdapters from 'components/lookup-tables/adapter-list/utils';
import type { SearchParams } from 'stores/PaginationTypes';
import type { LookupPreviewType } from 'components/lookup-tables/types';
import type { LookupTable, LookupTableAdapter, LookupTableCache } from 'logic/lookup-tables/types';

const _url = (path: string) => qualifyUrl(`/system/lookup/${path}`);

// Lookup Tables

export const fetchAllLookupTables = async (resolve: boolean = false): Promise<Array<LookupTable>> =>
  SystemLookup.tables(undefined, 1, 0, undefined, undefined, resolve).then((response: any) => response.lookup_tables);

export const deleteLookupTable = async (tableId: string) => SystemLookup.removeTable(tableId);

export const fetchErrors = async ({
  lutNames = undefined,
  cacheNames = undefined,
  adapterNames = undefined,
}: {
  lutNames?: Array<string>;
  cacheNames?: Array<string>;
  adapterNames?: Array<string>;
}) => {
  const request: { tables?: Array<string>; caches?: Array<string>; data_adapters?: Array<string> } = {};

  if (lutNames) request.tables = lutNames;
  if (cacheNames) request.caches = cacheNames;
  if (adapterNames) request.data_adapters = adapterNames;

  return fetch('POST', _url('errorstates'), request);
};

export const fetchPaginatedLookupTables = async (searchParams: SearchParams) => {
  const { page, pageSize, query, sort } = searchParams;
  const sortField = sort?.attributeId;
  const sortOrder = sort?.direction;

  const url = _url(
    PaginationURL('tables', page, pageSize, query, { resolve: true, sort: sortField, order: sortOrder }),
  );

  return fetch('GET', url).then(deserializeLookupTables);
};

export const fetchLookupTable = async (idOrName: string): Promise<{ lookup_tables: Array<LookupTable> }> =>
  SystemLookup.get(idOrName, true);

export const createLookupTable = async (payload: LookupTableCache) => fetch('POST', _url('tables'), payload);

export const updateLookupTable = async (payload: LookupTableCache) =>
  fetch('PUT', _url(`tables/${(payload as any).id}`), payload);

export const purgeLookupTableKey = async ({ table, key }: { table: LookupTable; key: string }) =>
  ClusterLookupTable.performPurge(table.id, key);

export const purgeAllLookupTableKey = async (table: LookupTable) => ClusterLookupTable.performPurge(table.id);

export const testLookupTableKey = async ({ tableName, key }: { tableName: string; key: string }) =>
  SystemLookup.performLookup(tableName, key);

export const fetchLookupPreview = async (idOrName: string, size: number): Promise<LookupPreviewType> =>
  fetch('GET', qualifyUrl(`/system/lookup/tables/preview/${idOrName}?size=${size}`));

// Caches

export const fetchPaginatedCaches = async (searchParams: SearchParams) => {
  const { page, pageSize, query, sort } = searchParams;
  const sortField = sort?.attributeId;
  const sortOrder = sort?.direction;

  const url = _url(PaginationURL('caches', page, pageSize, query, { sort: sortField, order: sortOrder }));

  return fetch('GET', url).then(deserializeCaches);
};

export const fetchCache = async (idOrName: string): Promise<LookupTableCache> => SystemLookup.getCache(idOrName);

export const fetchCacheTypes = async () => SystemLookup.availableCacheTypes();

export const validateCache = async (cache: LookupTableCache) => fetch('POST', _url('caches/validate'), cache);

export const createCache = async (payload: LookupTableCache) => fetch('POST', _url('caches'), payload);

export const updateCache = async (payload: LookupTableCache) =>
  fetch('PUT', _url(`caches/${(payload as any).id}`), payload);

export const deleteCache = async (cacheId: string) => SystemLookup.deleteCache(cacheId);

// Data Adapters

export const fetchPaginatedDataAdapters = async (searchParams: SearchParams) => {
  const { page, pageSize, query, sort } = searchParams;
  const sortField = sort?.attributeId;
  const sortOrder = sort?.direction;

  const url = _url(PaginationURL('adapters', page, pageSize, query, { sort: sortField, order: sortOrder }));

  return fetch('GET', url).then(deserializeDataAdapters);
};

export const fetchDataAdapter = async (idOrName: string): Promise<LookupTableAdapter> =>
  SystemLookup.getAdapter(idOrName);

export const fetchDataAdapterTypes = async () => SystemLookup.availableAdapterTypes();

export const createDataAdapter = async (payload: LookupTableCache) => fetch('POST', _url('adapters'), payload);

export const updateDataAdapter = async (payload: LookupTableCache) =>
  fetch('PUT', _url(`adapters/${(payload as any).id}`), payload);

export const validateDataAdapter = async (adapter: LookupTableAdapter) =>
  fetch('POST', _url('adapters/validate'), adapter);

export const deleteDataAdapter = async (adapterId: string) => SystemLookup.deleteAdapter(adapterId);

export const lookupDataAdapter = async (adapterName: string, key: string) =>
  fetch('GET', _url(`adapters/${adapterName}/query?key=${encodeURIComponent(key)}`));

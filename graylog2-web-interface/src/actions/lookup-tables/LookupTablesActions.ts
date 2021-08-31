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
import Reflux from 'reflux';

import { singletonActions } from 'views/logic/singleton';
import { LookupTable } from 'logic/lookup-tables/types';
import { RefluxActions } from 'stores/StoreTypes';

type LookupTableActionsType = {
  searchPaginated: (page: number, perPage: number, query: string, resolve: boolean) => Promise<unknown>,
  reloadPage: () => Promise<unknown>,
  get: (idOrName: string) => Promise<unknown>,
  create: (table: LookupTable) => Promise<unknown>,
  delete: (idOrName: string) => Promise<unknown>,
  update: (table: LookupTable) => Promise<unknown>,
  getErrors: (tableNames: Array<string> | undefined, cacheNames: Array<string> | undefined, dataAdapterNames: Array<string> | undefined) => Promise<unknown>,
  lookup: (tableName: string, key: string) => Promise<unknown>,
  purgeKey: (table: LookupTable, key: string) => Promise<unknown>,
  purgeAll: (table: LookupTable, key: string) => Promise<unknown>,
  validate: (table: LookupTable) => Promise<unknown>,
}

const LookupTablesActions: RefluxActions<LookupTableActionsType> = singletonActions('LookupTables', () => Reflux.createActions({
  searchPaginated: { asyncResult: true },
  reloadPage: { asyncResult: true },
  get: { asyncResult: true },
  create: { asyncResult: true },
  delete: { asyncResult: true },
  update: { asyncResult: true },
  getErrors: { asyncResult: true },
  lookup: { asyncResult: true },
  purgeKey: { asyncResult: true },
  purgeAll: { asyncResult: true },
  validate: { asyncResult: true },
}));

export default LookupTablesActions;

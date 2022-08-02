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

export const TABLE: LookupTable = {
  id: '62a9e6bdf3d7456348ef8e53',
  title: 'Watchlist (Internal - Do not remove)',
  description: 'A basic description',
  name: 'watchlist',
  _scope: null,
};

export const CACHE: LookupTableCache = {
  id: '62a9e6bdf3d7456348ef8e51',
  title: 'Watchlist (Internal - Do not remove)',
  description: 'A cache for the Watchlist entries to speed up lookup.',
  name: 'watchlist-cache',
  _scope: null,
};

export const DATA_ADAPTER: LookupTableAdapter = {
  id: '62a9e6bdf3d7456348ef8e4f',
  title: 'Watchlist (Internal - Do not remove)',
  description: 'The Lookup Adapter for the Watchlist.',
  name: 'watchlist-mongo',
  _scope: null,
};

export const mockedFetchScopePermissions = async () => {
  return new Promise((resolve: any) => {
    setTimeout(() => {
      return resolve({
        entity_scopes: {
          ILLUMINATE: { is_mutable: false },
          DEFAULT: { is_mutable: true },
        },
      });
    }, 1000);
  });
};

import type { LookupTable, LookupTableCache, LookupTableAdapter } from 'logic/lookup-tables/types';

export const TABLE: LookupTable = {
  id: '62a9e6bdf3d7456348ef8e53',
  title: 'Watchlist (Internal - Do not remove)',
  description: 'A basic description',
  name: 'watchlist',
  scope: null,
};

export const CACHE: LookupTableCache = {
  id: '62a9e6bdf3d7456348ef8e51',
  title: 'Watchlist (Internal - Do not remove)',
  description: 'A cache for the Watchlist entries to speed up lookup.',
  name: 'watchlist-cache',
  scope: null,
};

export const DATA_ADAPTER: LookupTableAdapter = {
  id: '62a9e6bdf3d7456348ef8e4f',
  title: 'Watchlist (Internal - Do not remove)',
  description: 'The Lookup Adapter for the Watchlist.',
  name: 'watchlist-mongo',
  scope: null,
};

export const MockedEntityScopesPermissions = {
  get: async () => {
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
  },
};

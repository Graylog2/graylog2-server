import type { LookupTable, LookupTableCache, LookupTableAdapter } from 'logic/lookup-tables/types';

export const TABLE: LookupTable = {
  id: '62a9e6bdf3d7456348ef8e53',
  title: 'Watchlist (Internal - Do not remove)',
  description: 'A basic description',
  name: 'watchlist',
  _metadata: null,
};

export const CACHE: LookupTableCache = {
  id: '62a9e6bdf3d7456348ef8e51',
  title: 'Watchlist (Internal - Do not remove)',
  description: 'A cache for the Watchlist entries to speed up lookup.',
  name: 'watchlist-cache',
  _metadata: null,
};

export const DATA_ADAPTER: LookupTableAdapter = {
  id: '62a9e6bdf3d7456348ef8e4f',
  title: 'Watchlist (Internal - Do not remove)',
  description: 'The Lookup Adapter for the Watchlist.',
  name: 'watchlist-mongo',
  _metadata: null,
};

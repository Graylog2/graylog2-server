import type { EntityBase } from 'components/common/EntityDataTable/types';
import type { LookupTable, LookupTableCache, LookupTableAdapter } from 'logic/lookup-tables/types';

export type LookupTableEntity = EntityBase & LookupTable;
export type CachesMap = { [key: string]: LookupTableCache };
export type AdaptersMap = { [key: string]: LookupTableAdapter };

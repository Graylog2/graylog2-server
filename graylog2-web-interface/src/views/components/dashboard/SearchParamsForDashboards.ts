import type { SearchParams } from 'stores/PaginationTypes';

export type SearchParamsForDashboards = SearchParams & {
  scope: 'read' | 'update';
};

import { useEffect } from 'react';

import type { DashboardsStoreState } from 'views/stores/DashboardsStore'; import { useStore } from 'stores/connect'; import { DashboardsStore, DashboardsActions } from 'views/stores/DashboardsStore';

export type Dashboards = DashboardsStoreState;

type SearchQuery = {
  query?: string,
  page?: number,
  perPage?: number
};

const useDashboards = (searchQuery: SearchQuery): Readonly<Dashboards> => {
  const dashboards = useStore(DashboardsStore);

  useEffect(() => {
    DashboardsActions.search(searchQuery?.query ?? '', searchQuery?.page ?? 1, searchQuery?.perPage ?? 10);
  }, [searchQuery]);

  return dashboards;
};

export default useDashboards;

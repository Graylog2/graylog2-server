import { useEffect } from 'react';

import type { DashboardsStoreState } from 'views/stores/DashboardsStore'; import { useStore } from 'stores/connect'; import { DashboardsStore, DashboardsActions } from 'views/stores/DashboardsStore';

export type Dashboards = DashboardsStoreState;

const useDashboards = (query: string = '', page: number = 1, perPage: number = 10): Readonly<Dashboards> => {
  const dashboards = useStore(DashboardsStore);

  useEffect(() => {
    DashboardsActions.search(query, page, perPage);
  }, [query, page, perPage]);

  return dashboards;
};

export default useDashboards;

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
import { useEffect } from 'react';

import type { DashboardsStoreState } from 'views/stores/DashboardsStore';
import { DashboardsActions, DashboardsStore } from 'views/stores/DashboardsStore';
import { useStore } from 'stores/connect';

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

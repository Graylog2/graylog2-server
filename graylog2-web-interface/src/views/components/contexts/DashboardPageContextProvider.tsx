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
import * as React from 'react';
import { useState, useEffect, useMemo, useCallback } from 'react';
import { useLocation, useHistory } from 'react-router-dom';
import URI from 'urijs';

import useQuery from 'routing/useQuery';
import DashboardPageContext from 'views/components/contexts/DashboardPageContext';
import { ViewActions } from 'views/stores/ViewStore';
import { ViewStatesStore } from 'views/stores/ViewStatesStore';
import { useStore } from 'stores/connect';

const _clearURI = (query) => new URI(query)
  .removeSearch('page');

const _updateQueryParams = (
  newPage: string | undefined,
  query: string,
) => {
  const baseUri = _clearURI(query);

  if (newPage) {
    return baseUri.setSearch('page', newPage).toString();
  }

  return baseUri.toString();
};

const useSyncStateWithQueryParams = ({ dashboardPage, uriParams, setDashboardPage, states }) => {
  useEffect(() => {
    const nextPage = uriParams.page;

    if (!states.has(nextPage)) {
      setDashboardPage(undefined);
    } else if (nextPage !== dashboardPage) {
      setDashboardPage(nextPage);
      ViewActions.selectQuery(nextPage);
    }
  }, [uriParams.page, dashboardPage, setDashboardPage, states]);
};

const useCleanupQueryParams = ({ uriParams, query, history }) => {
  useEffect(() => {
    if (uriParams?.page === undefined) {
      const baseURI = _clearURI(query);

      history.replace(baseURI.toString());
    }
  }, [query, history, uriParams?.page]);
};

const DashboardPageContextProvider = ({ children }: { children: React.ReactNode }): React.ReactElement => {
  const states = useStore(ViewStatesStore);
  const { search, pathname } = useLocation();
  const query = pathname + search;
  const history = useHistory();
  const [dashboardPage, setDashboardPage] = useState<string | undefined>();
  const params = useQuery();
  const uriParams = useMemo(() => ({
    page: params.page,
  }), [params]);

  useSyncStateWithQueryParams({ dashboardPage, uriParams, setDashboardPage, states });
  useCleanupQueryParams({ uriParams, query, history });

  const updatePageParams = useCallback((newPage: string | undefined) => {
    const newUri = _updateQueryParams(newPage, query);

    history.replace(newUri);
  }, [history, query]);

  const setDashboardPageParam = useCallback((nextPage) => updatePageParams(nextPage), [updatePageParams]);
  const unSetDashboardPageParam = useCallback(() => updatePageParams(undefined), [updatePageParams]);

  const dashboardPageContextValue = useMemo(() => ({
    setDashboardPage: setDashboardPageParam,
    unsetDashboardPage: unSetDashboardPageParam,
    dashboardPage: dashboardPage,
  }), [dashboardPage, setDashboardPageParam, unSetDashboardPageParam]);

  return (
    <DashboardPageContext.Provider value={dashboardPageContextValue}>
      {children}
    </DashboardPageContext.Provider>
  );
};

export default DashboardPageContextProvider;

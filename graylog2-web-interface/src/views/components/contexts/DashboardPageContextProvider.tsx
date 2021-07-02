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

const _clearURI = (query) => new URI(query)
  .removeSearch('page');

const _updateQueryParams = (
  newPage: string | undefined,
  query: string,
) => {
  let baseUri = _clearURI(query);

  if (newPage) {
    baseUri = baseUri.setSearch('page', newPage);
  }

  return baseUri.toString();
};

const useSyncStateWithQueryParams = ({ dashboardPage, uriParams, setDashboardPage }) => {
  useEffect(() => {
    const nextPage = uriParams.page;

    if (nextPage !== dashboardPage) {
      setDashboardPage(nextPage);
    }
  }, [uriParams.page, dashboardPage, setDashboardPage]);
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
  const { search, pathname } = useLocation();
  const query = pathname + search;
  const history = useHistory();
  const [dashboardPage, setDashboardPage] = useState<string | undefined>();
  const params = useQuery();
  const uriParams = useMemo(() => ({
    page: params.page,
  }), [params]);

  useSyncStateWithQueryParams({ dashboardPage, uriParams, setDashboardPage });
  useCleanupQueryParams({ uriParams, query, history });

  const updatePageParams = useCallback((newPage: string | undefined) => {
    const newUri = _updateQueryParams(newPage, query);

    history.replace(newUri);
  }, [history, query]);

  const setDashboardPageParam = (nextPage) => updatePageParams(nextPage);
  const unSetDashboardPageParam = () => updatePageParams(undefined);

  return (
    <DashboardPageContext.Provider value={{
      setDashboardPage: setDashboardPageParam,
      unsetDashboardPage: unSetDashboardPageParam,
      dashboardPage: dashboardPage,
    }}>
      {children}
    </DashboardPageContext.Provider>
  );
};

export default DashboardPageContextProvider;

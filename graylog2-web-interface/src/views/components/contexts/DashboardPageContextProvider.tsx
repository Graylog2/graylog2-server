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
import { useState, useEffect, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import URI from 'urijs';

import useLocation from 'routing/useLocation';
import useQuery from 'routing/useQuery';
import DashboardPageContext from 'views/components/contexts/DashboardPageContext';
import useAppSelector from 'stores/useAppSelector';
import { selectViewStates } from 'views/logic/slices/viewSelectors';
import useAppDispatch from 'stores/useAppDispatch';
import { selectQuery } from 'views/logic/slices/viewSlice';

const _clearURI = (query: string) => new URI(query).removeSearch('page');

const _updateQueryParams = (newPage: string | undefined, query: string) => {
  const baseUri = _clearURI(query);

  if (newPage) {
    return baseUri.setSearch('page', newPage).toString();
  }

  return baseUri.toString();
};

const useSyncStateWithQueryParams = ({ dashboardPage, uriParams, setDashboardPage }) => {
  const states = useAppSelector(selectViewStates);
  const dispatch = useAppDispatch();

  useEffect(() => {
    const nextPage = uriParams.page;

    if (!states?.has(nextPage)) {
      setDashboardPage(undefined);
    } else if (nextPage !== dashboardPage) {
      setDashboardPage(nextPage);
      dispatch(selectQuery(nextPage));
    }
  }, [uriParams.page, dashboardPage, setDashboardPage, states, dispatch]);
};

const useCleanupQueryParams = ({ uriParams, query, navigate }) => {
  useEffect(() => {
    if (uriParams?.page === undefined) {
      const baseURI = _clearURI(query);

      navigate(baseURI.toString(), { replace: true });
    }
  }, [query, navigate, uriParams?.page]);
};

const DashboardPageContextProvider = ({ children }: { children: React.ReactNode }): React.ReactElement => {
  const { search, pathname } = useLocation();
  const query = pathname + search;
  const navigate = useNavigate();
  const [dashboardPage, setDashboardPage] = useState<string | undefined>();
  const params = useQuery();
  const uriParams = useMemo(() => ({
    page: params.page,
  }), [params]);

  useSyncStateWithQueryParams({ dashboardPage, uriParams, setDashboardPage });
  useCleanupQueryParams({ uriParams, query, navigate });

  const dashboardPageContextValue = useMemo(() => {
    const updatePageParams = (newPage: string | undefined) => {
      const newUri = _updateQueryParams(newPage, query);

      navigate(newUri, { replace: true });
    };

    const setDashboardPageParam = (nextPage: string) => updatePageParams(nextPage);
    const unSetDashboardPageParam = () => updatePageParams(undefined);

    return ({
      setDashboardPage: setDashboardPageParam,
      unsetDashboardPage: unSetDashboardPageParam,
      dashboardPage: dashboardPage,
    });
  }, [dashboardPage, navigate, query]);

  return (
    <DashboardPageContext.Provider value={dashboardPageContextValue}>
      {children}
    </DashboardPageContext.Provider>
  );
};

export default DashboardPageContextProvider;

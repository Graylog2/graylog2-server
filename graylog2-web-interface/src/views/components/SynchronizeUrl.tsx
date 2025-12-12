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

import useSyncWithQueryParameters from 'views/hooks/useSyncWithQueryParameters';
import bindSearchParamsFromQuery from 'views/hooks/BindSearchParamsFromQuery';
import type { ViewsDispatch } from 'views/stores/useViewsDispatch';
import useViewsDispatch from 'views/stores/useViewsDispatch';
import type { RootState } from 'views/types';
import { selectView } from 'views/logic/slices/viewSelectors';
import { selectSearchExecutionState } from 'views/logic/slices/searchExecutionSelectors';
import useLocation from 'routing/useLocation';
import useQuery from 'routing/useQuery';
import { updateView } from 'views/logic/slices/viewSlice';

const bindSearchParamsFromQueryThunk =
  (query: { [key: string]: unknown }) => async (dispatch: ViewsDispatch, getState: () => RootState) => {
    const view = selectView(getState());
    const executionState = selectSearchExecutionState(getState());

    const result = await bindSearchParamsFromQuery({ view, query, retry: () => Promise.resolve(), executionState });

    if (!result) {
      return Promise.resolve();
    }

    const [newView] = result;

    if (newView !== view) {
      return dispatch(updateView(newView, true));
    }
  };

const useBindSearchParamsFromQuery = (query: { [key: string]: unknown }) => {
  const dispatch = useViewsDispatch();

  useEffect(() => {
    dispatch(bindSearchParamsFromQueryThunk(query));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [query]);
};

const SynchronizeUrl = () => {
  const { pathname, search } = useLocation();
  const query = useQuery();
  useBindSearchParamsFromQuery(query);
  useSyncWithQueryParameters(`${pathname}${search}`);

  return null;
};

export default SynchronizeUrl;

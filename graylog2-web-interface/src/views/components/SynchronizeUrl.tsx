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

import type { Location } from 'routing/withLocation';
import withLocation from 'routing/withLocation';
import { useSyncWithQueryParameters } from 'views/hooks/SyncWithQueryParameters';
import bindSearchParamsFromQuery from 'views/hooks/BindSearchParamsFromQuery';
import type { AppDispatch } from 'stores/useAppDispatch';
import type { RootState } from 'views/types';
import { selectView } from 'views/logic/slices/viewSelectors';
import useAppDispatch from 'stores/useAppDispatch';
import { selectSearchExecutionState } from 'views/logic/slices/searchExecutionSelectors';

const bindSearchParamsFromQueryThunk = (query: { [key: string]: unknown; }) => (_dispatch: AppDispatch, getState: () => RootState) => {
  const view = selectView(getState());
  const executionState = selectSearchExecutionState(getState());
  bindSearchParamsFromQuery({ view, query, retry: () => Promise.resolve(), executionState });
};

const useBindSearchParamsFromQuery = (query: { [key: string]: unknown }) => {
  const dispatch = useAppDispatch();

  useEffect(() => {
    dispatch(bindSearchParamsFromQueryThunk(query));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [query]);
};

type Props = {
  location: Location,
};

const SynchronizeUrl = ({ location }: Props) => {
  const { pathname, search } = location;
  const query = `${pathname}${search}`;
  useBindSearchParamsFromQuery(location.query);
  useSyncWithQueryParameters(query);

  return null;
};

export default withLocation(SynchronizeUrl);

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
import { useState, useEffect, useCallback, useMemo } from 'react';
import isEqual from 'lodash/isEqual';
import URI from 'urijs';

import useLocation from 'routing/useLocation';
import useQuery from 'routing/useQuery';
import useActiveQueryId from 'views/hooks/useActiveQueryId';
import useWidgets from 'views/hooks/useWidgets';
import useAppDispatch from 'stores/useAppDispatch';
import { execute, setWidgetsToSearch } from 'views/logic/slices/searchExecutionSlice';
import type { HistoryFunction } from 'routing/useHistory';
import useHistory from 'routing/useHistory';

import type { FocusContextState } from './WidgetFocusContext';
import WidgetFocusContext from './WidgetFocusContext';

type WidgetFocusRequest = {
  id: string,
  editing: false,
  focusing: true,
}

type WidgetEditRequest = {
  id: string,
  editing: boolean,
  focusing: boolean,
}

const _clearURI = (query: string) => new URI(query)
  .removeSearch('focusing')
  .removeSearch('editing')
  .removeSearch('focusedId');

const _updateQueryParams = (
  newQueryParams: WidgetFocusRequest | WidgetEditRequest | undefined,
  query: string,
) => {
  let baseUri = _clearURI(query);

  if (newQueryParams) {
    if (newQueryParams.id && (newQueryParams.focusing || newQueryParams.editing)) {
      baseUri = baseUri.setSearch('focusedId', newQueryParams.id);
    }

    if (newQueryParams.focusing) {
      baseUri = baseUri.setSearch('focusing', String(true));
    }

    if (newQueryParams.editing) {
      baseUri = baseUri.setSearch('editing', String(true));
    }
  }

  return baseUri.toString();
};

type FocusUriParams = {
  editing: boolean,
  focusing: boolean,
  id: string,
  isPageShown: boolean,
};
type SyncStateArgs = {
  focusedWidget: FocusContextState | undefined,
  setFocusedWidget: (newFocusedWidget: FocusContextState | undefined) => void,
  widgetIds: Array<string>,
  focusUriParams: FocusUriParams,
}

const emptyFocusContext: FocusContextState = {
  editing: false,
  focusing: false,
  id: undefined,
};

const useSyncStateWithQueryParams = ({ focusedWidget, focusUriParams, setFocusedWidget, widgetIds }: SyncStateArgs) => {
  const dispatch = useAppDispatch();

  useEffect(() => {
    const nextFocusedWidget = {
      id: focusUriParams.id,
      editing: focusUriParams.editing,
      focusing: focusUriParams.focusing || focusUriParams.editing,
    } as FocusContextState;

    if (!isEqual(focusedWidget ?? emptyFocusContext, nextFocusedWidget)) {
      if (focusUriParams.id && !widgetIds.includes(focusUriParams.id)) {
        return;
      }

      setFocusedWidget(nextFocusedWidget);
      const filter = nextFocusedWidget?.id ? [nextFocusedWidget.id] : undefined;
      dispatch(setWidgetsToSearch(filter));

      dispatch(execute());
    }
  }, [focusedWidget, setFocusedWidget, widgetIds, focusUriParams, dispatch]);
};

type CleanupArgs = {
  focusUriParams: FocusUriParams,
  widgetIds: Array<string>,
  query: string,
  history: HistoryFunction,
};

const useCleanupQueryParams = ({ focusUriParams, widgetIds, query, history }: CleanupArgs) => {
  useEffect(() => {
    if ((focusUriParams?.id && !widgetIds.includes(focusUriParams.id) && focusUriParams.isPageShown) || (focusUriParams?.id === undefined)) {
      const baseURI = _clearURI(query);

      history.replace(baseURI.toString());
    }
  }, [focusUriParams, widgetIds, query, history]);
};

const WidgetFocusProvider = ({ children }: { children: React.ReactNode }): React.ReactElement => {
  const { search, pathname } = useLocation();
  const query = pathname + search;
  const history = useHistory();
  const [focusedWidget, setFocusedWidget] = useState<FocusContextState | undefined>();
  const widgets = useWidgets();
  const widgetIds = useMemo(() => widgets.map((widget) => widget.id).toArray(), [widgets]);
  const activeQuery = useActiveQueryId();
  const params = useQuery();
  const focusUriParams = useMemo(() => ({
    editing: params.editing === 'true',
    focusing: params.focusing === 'true',
    id: params.focusedId as string,
    isPageShown: !params.page || params.page === activeQuery,
  }), [params.editing, params.focusing, params.focusedId, params.page, activeQuery]);

  useSyncStateWithQueryParams({ focusedWidget, setFocusedWidget, widgetIds, focusUriParams });

  useCleanupQueryParams({ focusUriParams, widgetIds, query, history });

  const updateFocusQueryParams = useCallback((newQueryParams: WidgetFocusRequest | WidgetEditRequest | undefined) => {
    const newURI = _updateQueryParams(
      newQueryParams,
      query,
    );

    history.replace(newURI);
  }, [history, query]);

  const setWidgetFocusing = useCallback((widgetId: string) => updateFocusQueryParams({
    id: widgetId,
    editing: false,
    focusing: true,
  }), [updateFocusQueryParams]);

  const unsetWidgetFocusing = useCallback(() => updateFocusQueryParams(undefined), [updateFocusQueryParams]);

  const setWidgetEditing = useCallback((widgetId: string) => {
    updateFocusQueryParams({
      id: widgetId,
      editing: true,
      focusing: focusUriParams.focusing,
    });
  }, [focusUriParams.focusing, updateFocusQueryParams]);

  const unsetWidgetEditing = useCallback(() => updateFocusQueryParams({
    id: focusUriParams.focusing && focusUriParams.id ? focusUriParams.id as string : undefined,
    editing: false,
    focusing: focusUriParams.focusing,
  }), [focusUriParams.focusing, focusUriParams.id, updateFocusQueryParams]);

  const widgetFocusContextValue = useMemo(() => ({
    focusedWidget,
    setWidgetFocusing,
    setWidgetEditing,
    unsetWidgetEditing,
    unsetWidgetFocusing,
  }), [
    focusedWidget,
    setWidgetFocusing,
    setWidgetEditing,
    unsetWidgetEditing,
    unsetWidgetFocusing,
  ]);

  return (
    <WidgetFocusContext.Provider value={widgetFocusContextValue}>
      {children}
    </WidgetFocusContext.Provider>
  );
};

export default WidgetFocusProvider;

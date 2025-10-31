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
import useViewsDispatch from 'views/stores/useViewsDispatch';
import type { HistoryFunction } from 'routing/useHistory';
import useHistory from 'routing/useHistory';
import { executeActiveQuery, setWidgetToSearch } from 'views/logic/slices/viewSlice';
import { selectSearchTypesToSearch } from 'views/logic/slices/searchExecutionSelectors';
import useAppSelector from 'stores/useAppSelector';
import useView from 'views/hooks/useView';
import { setSearchTypesToSearch } from 'views/logic/slices/searchExecutionSlice';
import { setNewWidget } from 'views/logic/slices/widgetsSlice';

import type { FocusContextState } from './WidgetFocusContext';
import WidgetFocusContext from './WidgetFocusContext';

type WidgetFocusRequest = {
  id: string;
  editing: false;
  focusing: true;
};

type WidgetEditRequest = {
  id: string;
  editing: boolean;
  focusing: boolean;
};

const _clearURI = (query: string) =>
  new URI(query).removeSearch('focusing').removeSearch('editing').removeSearch('focusedId');

const _updateQueryParams = (newQueryParams: WidgetFocusRequest | WidgetEditRequest | undefined, query: string) => {
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
  editing: boolean;
  focusing: boolean;
  id: string;
  isPageShown: boolean;
};
type SyncStateArgs = {
  focusedWidget: FocusContextState | undefined;
  setFocusedWidget: (newFocusedWidget: FocusContextState | undefined) => void;
  widgetIds: Array<string>;
  focusUriParams: FocusUriParams;
};

const emptyFocusContext = {
  editing: false,
  focusing: false,
  id: undefined,
} as const;

const useSyncStateWithQueryParams = ({ focusedWidget, focusUriParams, setFocusedWidget, widgetIds }: SyncStateArgs) => {
  const dispatch = useViewsDispatch();
  const { widgetMapping } = useView();
  const searchTypesToSearch = useAppSelector(selectSearchTypesToSearch);

  useEffect(() => {
    const nextFocusedWidget: FocusContextState =
      focusUriParams.focusing || focusUriParams.editing
        ? {
            id: focusUriParams.id,
            editing: focusUriParams.editing,
            focusing: true,
          }
        : emptyFocusContext;

    if (!isEqual(focusedWidget ?? emptyFocusContext, nextFocusedWidget)) {
      if (focusUriParams.id && !widgetIds.includes(focusUriParams.id)) {
        return;
      }

      setFocusedWidget(nextFocusedWidget);
      dispatch(setWidgetToSearch(nextFocusedWidget.id));
      dispatch(executeActiveQuery());
    }
  }, [focusedWidget, setFocusedWidget, widgetIds, focusUriParams, dispatch]);

  useEffect(() => {
    if (focusedWidget) {
      const searchTypeIds = widgetMapping.get(focusedWidget.id)?.toArray() ?? [];
      const searchTypesToSearchIsUpToDate =
        searchTypeIds.length === 0 ||
        !searchTypesToSearch ||
        searchTypesToSearch.length === 0 ||
        searchTypeIds.every((id) => searchTypesToSearch?.includes(id));
      if (!searchTypesToSearchIsUpToDate) {
        dispatch(setSearchTypesToSearch(searchTypeIds));
      }
    }
  }, [dispatch, focusedWidget, searchTypesToSearch, widgetMapping]);
};

type CleanupArgs = {
  focusUriParams: FocusUriParams;
  widgetIds: Array<string>;
  query: string;
  history: HistoryFunction;
};

const useCleanupQueryParams = ({ focusUriParams, widgetIds, query, history }: CleanupArgs) => {
  useEffect(() => {
    if (
      (focusUriParams?.id && !widgetIds.includes(focusUriParams.id) && focusUriParams.isPageShown) ||
      focusUriParams?.id === undefined
    ) {
      const baseURI = _clearURI(query);
      const newQuery = baseURI.toString();

      if (query !== newQuery) {
        history.replace(newQuery);
      }
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
  const focusUriParams = useMemo(
    () => ({
      editing: params.editing === 'true',
      focusing: params.focusing === 'true',
      id: params.focusedId as string,
      isPageShown: !params.page || params.page === activeQuery,
    }),
    [params.editing, params.focusing, params.focusedId, params.page, activeQuery],
  );
  const dispatch = useViewsDispatch();

  useSyncStateWithQueryParams({ focusedWidget, setFocusedWidget, widgetIds, focusUriParams });

  useCleanupQueryParams({ focusUriParams, widgetIds, query, history });

  const updateFocusQueryParams = useCallback(
    (newQueryParams: WidgetFocusRequest | WidgetEditRequest | undefined) => {
      const newURI = _updateQueryParams(newQueryParams, query);

      if (newURI !== query) {
        history.replace(newURI);
      }
    },
    [history, query],
  );

  const setWidgetFocusing = useCallback(
    (widgetId: string) =>
      updateFocusQueryParams({
        id: widgetId,
        editing: false,
        focusing: true,
      }),
    [updateFocusQueryParams],
  );

  const unsetWidgetFocusing = useCallback(() => {
    if (focusUriParams.id) {
      dispatch(setNewWidget(focusUriParams.id));
    }
    updateFocusQueryParams(undefined);
  }, [dispatch, focusUriParams.id, updateFocusQueryParams]);

  const setWidgetEditing = useCallback(
    (widgetId: string) => {
      updateFocusQueryParams({
        id: widgetId,
        editing: true,
        focusing: focusUriParams.focusing,
      });
    },
    [focusUriParams.focusing, updateFocusQueryParams],
  );

  const unsetWidgetEditing = useCallback(() => {
    if (!focusUriParams.focusing && focusUriParams.id) {
      dispatch(setNewWidget(focusUriParams.id));
    }
    updateFocusQueryParams({
      id: focusUriParams.focusing && focusUriParams.id ? (focusUriParams.id as string) : undefined,
      editing: false,
      focusing: focusUriParams.focusing,
    });
  }, [dispatch, focusUriParams.focusing, focusUriParams.id, updateFocusQueryParams]);

  const widgetFocusContextValue = useMemo(
    () => ({
      focusedWidget,
      setWidgetFocusing,
      setWidgetEditing,
      unsetWidgetEditing,
      unsetWidgetFocusing,
    }),
    [focusedWidget, setWidgetFocusing, setWidgetEditing, unsetWidgetEditing, unsetWidgetFocusing],
  );

  return <WidgetFocusContext.Provider value={widgetFocusContextValue}>{children}</WidgetFocusContext.Provider>;
};

export default WidgetFocusProvider;

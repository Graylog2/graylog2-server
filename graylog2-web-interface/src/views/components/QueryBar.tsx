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
import { useCallback, useContext } from 'react';
import type * as Immutable from 'immutable';

import DashboardPageContext from 'views/components/contexts/DashboardPageContext';
import ConfirmDeletingDashboardPage from 'views/logic/views/ConfirmDeletingDashboardPage';
import useQueryIds from 'views/hooks/useQueryIds';
import useQueryTitles from 'views/hooks/useQueryTitles';
import useViewMetadata from 'views/hooks/useViewMetadata';
import type { AppDispatch } from 'stores/useAppDispatch';
import useAppDispatch from 'stores/useAppDispatch';
import { selectQuery, createQuery, removeQuery } from 'views/logic/slices/viewSlice';
import useWidgetIds from 'views/components/useWidgetIds';
import { setTitle } from 'views/logic/slices/titlesActions';

import QueryTabs from './QueryTabs';

const onRemovePage = async (dashboardId: string, queryId: string, activeQueryId: string, queries: Immutable.OrderedSet<string>, widgetIds: Immutable.Map<string, Immutable.List<string>>, dispatch: AppDispatch) => {
  if (queries.size === 1) {
    return Promise.resolve();
  }

  const result = await ConfirmDeletingDashboardPage(dashboardId, activeQueryId, widgetIds);

  if (result === true) {
    dispatch(removeQuery(queryId));
  }

  return Promise.resolve();
};

const QueryBar = () => {
  const queries = useQueryIds();
  const queryTitles = useQueryTitles();
  const { activeQuery: activeQueryId, id: dashboardId } = useViewMetadata();
  const { setDashboardPage } = useContext(DashboardPageContext);
  const widgetIds = useWidgetIds();
  const dispatch = useAppDispatch();

  const onSelectPage = useCallback((pageId: string) => {
    if (pageId === 'new') {
      dispatch(createQuery()).then((newPageId) => setDashboardPage(newPageId));
    } else {
      setDashboardPage(pageId);
      dispatch(selectQuery(pageId));
    }
  }, [dispatch, setDashboardPage]);

  const removePage = useCallback(
    (queryId: string) => onRemovePage(
      dashboardId,
      queryId,
      activeQueryId,
      queries,
      widgetIds,
      dispatch,
    ),
    [dashboardId, activeQueryId, queries, widgetIds, dispatch],
  );

  const _onTitleChange = useCallback((queryId: string, newTitle: string) => dispatch(setTitle(queryId, 'tab', 'title', newTitle)), [dispatch]);

  return (
    <QueryTabs queries={queries}
               titles={queryTitles}
               dashboardId={dashboardId}
               onSelect={onSelectPage}
               onTitleChange={_onTitleChange}
               onRemove={removePage} />
  );
};

export default QueryBar;

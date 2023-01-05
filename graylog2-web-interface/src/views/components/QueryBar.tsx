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
import { List } from 'immutable';

import { useStore } from 'stores/connect';
import { TitlesActions } from 'views/stores/TitlesStore';
import NewQueryActionHandler from 'views/logic/NewQueryActionHandler';
import { QueriesActions } from 'views/stores/QueriesStore';
import { ViewStatesActions, ViewStatesStore } from 'views/stores/ViewStatesStore';
import DashboardPageContext from 'views/components/contexts/DashboardPageContext';
import FindNewActiveQueryId from 'views/logic/views/FindNewActiveQuery';
import ConfirmDeletingDashboardPage from 'views/logic/views/ConfirmDeletingDashboardPage';
import useQueryIds from 'views/hooks/useQueryIds';
import useQueryTitles from 'views/hooks/useQueryTitles';
import useViewMetadata from 'views/hooks/useViewMetadata';
import useAppDispatch from 'stores/useAppDispatch';
import { selectQuery } from 'views/logic/slices/viewSlice';

import QueryTabs from './QueryTabs';

const onTitleChange = (_queryId: string, newTitle: string) => TitlesActions.set('tab', 'title', newTitle);

const onCloseTab = async (dashboardId: string, queryId: string, activeQueryId: string, queries: Immutable.OrderedSet<string>, widgetIds: Immutable.Map<string, Immutable.List<string>>, setDashboardPage: (page: string) => void) => {
  if (queries.size === 1) {
    return Promise.resolve();
  }

  const result = await ConfirmDeletingDashboardPage(dashboardId, activeQueryId, widgetIds);

  if (result === true) {
    if (queryId === activeQueryId) {
      const indexedQueryIds = queries.toIndexedSeq();
      const newActiveQueryId = FindNewActiveQueryId(List(indexedQueryIds), activeQueryId);

      setDashboardPage(newActiveQueryId);
    }

    return QueriesActions.remove(queryId).then(() => ViewStatesActions.remove(queryId));
  }

  return Promise.resolve();
};

const useWidgetIds = () => useStore(ViewStatesStore, (states) => states.map((viewState) => viewState.widgets.map((widget) => widget.id).toList()).toMap());

const QueryBar = () => {
  const queries = useQueryIds();
  const queryTitles = useQueryTitles();
  const { activeQuery: activeQueryId, id: dashboardId } = useViewMetadata();
  const { setDashboardPage } = useContext(DashboardPageContext);
  const widgetIds = useWidgetIds();
  const dispatch = useAppDispatch();

  const onSelectPage = useCallback((pageId) => {
    if (pageId === 'new') {
      NewQueryActionHandler().then((newPage) => {
        setDashboardPage(newPage.id);
        dispatch(selectQuery(newPage.id));
      });
    }

    setDashboardPage(pageId);

    dispatch(selectQuery(pageId));
  }, [dispatch, setDashboardPage]);

  const onRemove = useCallback((queryId: string) => onCloseTab(dashboardId, queryId, activeQueryId, queries, widgetIds, setDashboardPage),
    [activeQueryId, dashboardId, queries, widgetIds, setDashboardPage]);

  return (
    <QueryTabs queries={queries}
               activeQueryId={activeQueryId}
               titles={queryTitles}
               dashboardId={dashboardId}
               onSelect={onSelectPage}
               onTitleChange={onTitleChange}
               onRemove={onRemove} />
  );
};

export default QueryBar;

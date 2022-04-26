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
import PropTypes from 'prop-types';
import type * as Immutable from 'immutable';
import * as ImmutablePropTypes from 'react-immutable-proptypes';
import { PluginStore } from 'graylog-web-plugin/plugin';

import connect, { useStore } from 'stores/connect';
import { TitlesActions } from 'views/stores/TitlesStore';
import { ViewActions } from 'views/stores/ViewStore';
import NewQueryActionHandler from 'views/logic/NewQueryActionHandler';
import { QueriesActions } from 'views/stores/QueriesStore';
import { QueryIdsStore } from 'views/stores/QueryIdsStore';
import { QueryTitlesStore } from 'views/stores/QueryTitlesStore';
import type { ViewMetaData } from 'views/stores/ViewMetadataStore';
import { ViewMetadataStore } from 'views/stores/ViewMetadataStore';
import { ViewStatesActions, ViewStatesStore } from 'views/stores/ViewStatesStore';
import type { QueryId } from 'views/logic/queries/Query';
import DashboardPageContext from 'views/components/contexts/DashboardPageContext';
import iterateConfirmationHooks from 'views/hooks/IterateConfirmationHooks';

import QueryTabs from './QueryTabs';

const onTitleChange = (_queryId: string, newTitle: string) => TitlesActions.set('tab', 'title', newTitle);

// eslint-disable-next-line no-alert
const defaultConfirm = async () => window.confirm('Do you really want to delete this dashboard page?');

const onCloseTab = async (dashboardId: string, queryId: string, currentQuery: string, queries: Immutable.OrderedSet<string>, widgetIds: Immutable.Map<string, Immutable.List<string>>, setDashboardPage: (page: string) => void) => {
  if (queries.size === 1) {
    return Promise.resolve();
  }

  const deletingDashboardPageHooks = PluginStore.exports('views.hooks.confirmDeletingDashboardPage');
  const _widgetIds = widgetIds.map((ids) => ids.toArray()).toObject();
  const result = await iterateConfirmationHooks([...deletingDashboardPageHooks, defaultConfirm], dashboardId, queryId, _widgetIds);

  if (result === true) {
    if (queryId === currentQuery) {
      const indexedQueryIds = queries.toIndexedSeq();
      const currentQueryIdIndex = indexedQueryIds.indexOf(queryId);
      const newQueryIdIndex = Math.min(0, currentQueryIdIndex - 1);
      const newQuery = indexedQueryIds.filter((currentQueryId) => (currentQueryId !== queryId))
        .get(newQueryIdIndex);

      setDashboardPage(newQuery);
    }

    return QueriesActions.remove(queryId).then(() => ViewStatesActions.remove(queryId));
  }

  return Promise.resolve();
};

type Props = {
  queries: Immutable.OrderedSet<QueryId>,
  queryTitles: Immutable.Map<string, string>,
  viewMetadata: ViewMetaData,
};

const useWidgetIds = () => useStore(ViewStatesStore, (states) => states.map((viewState) => viewState.widgets.map((widget) => widget.id).toList()).toMap());

const QueryBar = ({ queries, queryTitles, viewMetadata }: Props) => {
  const { activeQuery, id: dashboardId } = viewMetadata;
  const { setDashboardPage } = useContext(DashboardPageContext);
  const widgetIds = useWidgetIds();

  const onSelectPage = useCallback((pageId) => {
    if (pageId === 'new') {
      return NewQueryActionHandler().then((newPage) => {
        setDashboardPage(newPage.id);

        return newPage;
      });
    }

    setDashboardPage(pageId);

    return ViewActions.selectQuery(pageId);
  }, [setDashboardPage]);

  const onRemove = useCallback((queryId: string) => onCloseTab(dashboardId, queryId, activeQuery, queries, widgetIds, setDashboardPage),
    [activeQuery, dashboardId, queries, widgetIds, setDashboardPage]);

  return (
    <QueryTabs queries={queries}
               selectedQueryId={activeQuery}
               titles={queryTitles}
               onSelect={onSelectPage}
               onTitleChange={onTitleChange}
               onRemove={onRemove} />
  );
};

QueryBar.propTypes = {
  queries: ImmutablePropTypes.orderedSetOf(PropTypes.string).isRequired,
  queryTitles: ImmutablePropTypes.mapOf(PropTypes.string, PropTypes.string).isRequired,
  viewMetadata: PropTypes.exact({
    id: PropTypes.string,
    title: PropTypes.string,
    description: PropTypes.string,
    summary: PropTypes.string,
    activeQuery: PropTypes.string.isRequired,
  }).isRequired,
};

export default connect(QueryBar, {
  queries: QueryIdsStore,
  queryTitles: QueryTitlesStore,
  viewMetadata: ViewMetadataStore,
});

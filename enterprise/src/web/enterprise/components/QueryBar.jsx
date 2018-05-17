import React from 'react';
import PropTypes from 'prop-types';

import connect from 'stores/connect';
import { TitlesActions } from 'enterprise/stores/TitlesStore';
import { ViewActions, ViewStore } from 'enterprise/stores/ViewStore';
import NewQueryActionHandler from 'enterprise/logic/NewQueryActionHandler';
import { ViewManagementActions } from 'enterprise/stores/ViewManagementStore';
import UserNotification from 'util/UserNotification';
import Routes from 'routing/Routes';
import { QueriesActions } from '../stores/QueriesStore';
import { QueryIdsStore } from '../stores/QueryIdsStore';
import { QueryTitlesStore } from '../stores/QueryTitlesStore';
import { ViewMetadataStore } from '../stores/ViewMetadataStore';
import QueryTabs from './QueryTabs';

const onTitleChange = (queryId, newTitle) => TitlesActions.set('tab', 'title', newTitle);

const onSelectQuery = (queryId, executeSearch) => {
  if (queryId === 'new') {
    NewQueryActionHandler().then(executeSearch);
  } else {
    ViewActions.selectQuery(queryId);
  }
};

const onSaveView = (view) => {
  ViewManagementActions.save(view)
    .then(savedView => this.props.router.push(Routes.pluginRoute('VIEWS_VIEWID')(savedView.id)))
    .then(() => UserNotification.success(`Saving view "${view.title}" was successful!`, 'Success!'))
    .then(() => ViewStore.load(view));
};

const onCloseTab = (queryId, currentQuery, queries) => {
  if (queries.size === 1) {
    return;
  }
  QueriesActions.remove(queryId);
  if (queryId === currentQuery) {
    const newQuery = queries.remove(queryId).first();
    ViewActions.selectQuery(newQuery);
  }
};

const QueryBar = ({ children, onExecute, queries, queryTitles, viewMetadata }) => {
  const { activeQuery } = viewMetadata;
  const childrenWithQueryId = React.cloneElement(children, { queryId: activeQuery });
  const selectQueryAndExecute = queryId => onSelectQuery(queryId, onExecute);
  return (
    <QueryTabs queries={queries}
               selectedQuery={activeQuery}
               titles={queryTitles}
               onSelect={selectQueryAndExecute}
               onTitleChange={onTitleChange}
               onSaveView={onSaveView}
               onRemove={queryId => onCloseTab(queryId, activeQuery, queries)}>
      {childrenWithQueryId}
    </QueryTabs>
  );
};


export default connect(QueryBar, { queries: QueryIdsStore, queryTitles: QueryTitlesStore, viewMetadata: ViewMetadataStore });
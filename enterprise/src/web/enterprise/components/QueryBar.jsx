import React from 'react';
import PropTypes from 'prop-types';
import { withRouter } from 'react-router';

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
import CustomPropTypes from './CustomPropTypes';

const onTitleChange = (queryId, newTitle) => TitlesActions.set('tab', 'title', newTitle);

const onSelectQuery = (queryId, executeSearch) => {
  if (queryId === 'new') {
    NewQueryActionHandler().then(executeSearch);
  } else {
    ViewActions.selectQuery(queryId);
  }
};

const onSaveView = (view, router) => {
  ViewManagementActions.save(view)
    .then(savedView => router.push(Routes.pluginRoute('VIEWS_VIEWID')(savedView.id)))
    .then(() => UserNotification.success(`Saving view "${view.title}" was successful!`, 'Success!'))
    .then(() => ViewStore.load(view));
};

const onCloseTab = (queryId, currentQuery, queries) => {
  if (queries.size === 1) {
    return;
  }
  QueriesActions.remove(queryId);
  if (queryId === currentQuery) {
    const currentQueryIdIndex = queries.indexOf(queryId);
    const newQueryIdIndex = Math.min(0, currentQueryIdIndex - 1);
    const newQuery = queries.remove(queryId).get(newQueryIdIndex);
    ViewActions.selectQuery(newQuery);
  }
};

const QueryBar = ({ children, onExecute, queries, queryTitles, router, viewMetadata }) => {
  const { activeQuery } = viewMetadata;
  const childrenWithQueryId = React.cloneElement(children, { queryId: activeQuery });
  const selectQueryAndExecute = queryId => onSelectQuery(queryId, onExecute);
  return (
    <QueryTabs queries={queries}
               selectedQuery={activeQuery}
               titles={queryTitles}
               onSelect={selectQueryAndExecute}
               onTitleChange={onTitleChange}
               onSaveView={view => onSaveView(view, router)}
               onRemove={queryId => onCloseTab(queryId, activeQuery, queries)}>
      {childrenWithQueryId}
    </QueryTabs>
  );
};

QueryBar.propTypes = {
  children: CustomPropTypes.OneOrMoreChildren.isRequired,
  onExecute: PropTypes.func.isRequired,
  queries: PropTypes.object.isRequired,
  queryTitles: PropTypes.object.isRequired,
  router: PropTypes.any.isRequired,
  viewMetadata: PropTypes.shape({
    activeQuery: PropTypes.string.isRequired,
  }).isRequired,
};

export default withRouter(connect(QueryBar, { queries: QueryIdsStore, queryTitles: QueryTitlesStore, viewMetadata: ViewMetadataStore }));

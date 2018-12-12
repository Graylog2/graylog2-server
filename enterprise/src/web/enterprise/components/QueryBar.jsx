import React from 'react';
import PropTypes from 'prop-types';
import { withRouter } from 'react-router';

import connect from 'stores/connect';
import { TitlesActions } from 'enterprise/stores/TitlesStore';
import { ViewActions } from 'enterprise/stores/ViewStore';
import NewQueryActionHandler from 'enterprise/logic/NewQueryActionHandler';
import onSaveView from 'enterprise/logic/views/OnSaveViewAction';
import { QueriesActions } from 'enterprise/stores/QueriesStore';
import { QueryIdsStore } from 'enterprise/stores/QueryIdsStore';
import { QueryTitlesStore } from 'enterprise/stores/QueryTitlesStore';
import { ViewMetadataStore } from 'enterprise/stores/ViewMetadataStore';
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
  const childrenWithQueryId = React.Children.map(children, child => React.cloneElement(child, { queryId: activeQuery }));
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

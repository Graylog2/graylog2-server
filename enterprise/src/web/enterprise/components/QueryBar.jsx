// @flow strict
import React from 'react';
import PropTypes from 'prop-types';
// $FlowFixMe: imports from core need to be fixed in flow
import { withRouter } from 'react-router';

// $FlowFixMe: imports from core need to be fixed in flow
import connect from 'stores/connect';
import { TitlesActions } from 'enterprise/stores/TitlesStore';
import { ViewActions } from 'enterprise/stores/ViewStore';
import NewQueryActionHandler from 'enterprise/logic/NewQueryActionHandler';
import onSaveView from 'enterprise/logic/views/OnSaveViewAction';
import onSaveAsView from 'enterprise/logic/views/OnSaveAsViewAction';
import { QueriesActions } from 'enterprise/stores/QueriesStore';
import { QueryIdsStore } from 'enterprise/stores/QueryIdsStore';
import { QueryTitlesStore } from 'enterprise/stores/QueryTitlesStore';
import { ViewMetadataStore } from 'enterprise/stores/ViewMetadataStore';
import { ViewStatesActions } from 'enterprise/stores/ViewStatesStore';
import QueryTabs from './QueryTabs';
import CustomPropTypes from './CustomPropTypes';

const onTitleChange = (queryId, newTitle) => TitlesActions.set('tab', 'title', newTitle);

const onSelectQuery = queryId => (queryId === 'new' ? NewQueryActionHandler() : ViewActions.selectQuery(queryId));

const onCloseTab = (queryId, currentQuery, queries) => {
  if (queries.size === 1) {
    return Promise.resolve();
  }
  let promise;
  if (queryId === currentQuery) {
    const currentQueryIdIndex = queries.indexOf(queryId);
    const newQueryIdIndex = Math.min(0, currentQueryIdIndex - 1);
    const newQuery = queries.remove(queryId).get(newQueryIdIndex);
    promise = ViewActions.selectQuery(newQuery);
  } else {
    promise = Promise.resolve();
  }
  return promise.then(() => QueriesActions.remove(queryId)).then(() => ViewStatesActions.remove(queryId));
};

const QueryBar = ({ children, queries, queryTitles, router, viewMetadata }) => {
  const { activeQuery } = viewMetadata;
  const childrenWithQueryId = React.Children.map(children, child => React.cloneElement(child, { queryId: activeQuery }));
  const selectQueryAndExecute = queryId => onSelectQuery(queryId);
  return (
    <QueryTabs queries={queries}
               selectedQuery={activeQuery}
               titles={queryTitles}
               onSelect={selectQueryAndExecute}
               onTitleChange={onTitleChange}
               onSaveView={view => onSaveView(view, router)}
               onSaveAsView={view => onSaveAsView(view, router)}
               onRemove={queryId => onCloseTab(queryId, activeQuery, queries)}>
      {childrenWithQueryId}
    </QueryTabs>
  );
};

QueryBar.propTypes = {
  children: CustomPropTypes.OneOrMoreChildren.isRequired,
  queries: PropTypes.object.isRequired,
  queryTitles: PropTypes.object.isRequired,
  router: PropTypes.any.isRequired,
  viewMetadata: PropTypes.shape({
    activeQuery: PropTypes.string.isRequired,
  }).isRequired,
};

export default withRouter(connect(QueryBar, { queries: QueryIdsStore, queryTitles: QueryTitlesStore, viewMetadata: ViewMetadataStore }));

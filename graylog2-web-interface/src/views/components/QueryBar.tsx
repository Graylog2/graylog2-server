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
import React from 'react';
import PropTypes from 'prop-types';
import * as Immutable from 'immutable';
import * as ImmutablePropTypes from 'react-immutable-proptypes';
import { OrderedSet } from 'immutable';

import connect from 'stores/connect';
import { TitlesActions } from 'views/stores/TitlesStore';
import { ViewActions } from 'views/stores/ViewStore';
import NewQueryActionHandler from 'views/logic/NewQueryActionHandler';
import { QueriesActions } from 'views/stores/QueriesStore';
import { QueryIdsStore } from 'views/stores/QueryIdsStore';
import { QueryTitlesStore } from 'views/stores/QueryTitlesStore';
import { ViewMetaData, ViewMetadataStore } from 'views/stores/ViewMetadataStore';
import { ViewStatesActions } from 'views/stores/ViewStatesStore';
import { QueryId } from 'views/logic/queries/Query';

import QueryTabs from './QueryTabs';

const onTitleChange = (queryId, newTitle) => TitlesActions.set('tab', 'title', newTitle);

const onSelectQuery = (queryId) => (queryId === 'new' ? NewQueryActionHandler() : ViewActions.selectQuery(queryId));

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

type Props = {
  queries: OrderedSet<QueryId>,
  queryTitles: Immutable.Map<string, string>,
  viewMetadata: ViewMetaData,
};

const QueryBar = ({ queries, queryTitles, viewMetadata }: Props) => {
  const { activeQuery } = viewMetadata;
  const selectQueryAndExecute = (queryId) => onSelectQuery(queryId);

  return (
    <QueryTabs queries={queries}
               selectedQueryId={activeQuery}
               titles={queryTitles}
               onSelect={selectQueryAndExecute}
               onTitleChange={onTitleChange}
               onRemove={(queryId) => onCloseTab(queryId, activeQuery, queries)} />
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

export default connect(QueryBar, { queries: QueryIdsStore, queryTitles: QueryTitlesStore, viewMetadata: ViewMetadataStore });

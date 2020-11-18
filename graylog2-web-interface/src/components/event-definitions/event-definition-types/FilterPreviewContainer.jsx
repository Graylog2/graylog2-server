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
import lodash from 'lodash';
import uuid from 'uuid/v4';

import Query from 'views/logic/queries/Query';
import Search from 'views/logic/search/Search';
import CombinedProvider from 'injection/CombinedProvider';
import connect from 'stores/connect';
import PermissionsMixin from 'util/PermissionsMixin';

import FilterPreview from './FilterPreview';

const { FilterPreviewStore, FilterPreviewActions } = CombinedProvider.get('FilterPreview');
const { CurrentUserStore } = CombinedProvider.get('CurrentUser');

class FilterPreviewContainer extends React.Component {
  state = {
    queryId: uuid(),
    searchTypeId: uuid(),
  };

  fetchSearch = lodash.debounce((config) => {
    const { currentUser } = this.props;

    if (!this.isPermittedToSeePreview(currentUser, config)) {
      return;
    }

    const { queryId, searchTypeId } = this.state;

    const formattedStreams = config.streams.map((stream) => ({ type: 'stream', id: stream }));

    const queryBuilder = Query.builder()
      .id(queryId)
      .query({ type: 'elasticsearch', query_string: config.query || '*' })
      .timerange({ type: 'relative', range: config.search_within_ms / 1000 })
      .filter(formattedStreams.length === 0 ? null : { type: 'or', filters: formattedStreams })
      .searchTypes([{
        id: searchTypeId,
        type: 'messages',
        limit: 10,
        offset: 0,
      }]);

    const query = queryBuilder.build();

    const search = Search.create().toBuilder()
      .parameters(config.query_parameters.filter((param) => (!param.embryonic)))
      .queries([query])
      .build();

    FilterPreviewActions.search(search);
  }, 250);

  static propTypes = {
    eventDefinition: PropTypes.object.isRequired,
    filterPreview: PropTypes.object.isRequired,
    currentUser: PropTypes.object.isRequired,
  };

  componentDidMount() {
    const { eventDefinition } = this.props;

    this.fetchSearch(eventDefinition.config);
  }

  componentDidUpdate(prevProps) {
    const { eventDefinition } = this.props;

    const { query: prevQuery, query_parameters: prevQueryParameters, streams: prevStreams, search_within_ms: prevSearchWithin } = prevProps.eventDefinition.config;
    const { query, query_parameters: queryParameters, streams, search_within_ms: searchWithin } = eventDefinition.config;

    if (query !== prevQuery || queryParameters !== prevQueryParameters || !lodash.isEqual(streams, prevStreams) || searchWithin !== prevSearchWithin) {
      this.fetchSearch(eventDefinition.config);
    }
  }

  isPermittedToSeePreview = (currentUser, config) => {
    const missingPermissions = config.streams.some((stream) => {
      return !PermissionsMixin.isPermitted(currentUser.permissions, `streams:read:${stream}`);
    });

    return !missingPermissions;
  };

  render() {
    const { eventDefinition, filterPreview, currentUser } = this.props;
    const { queryId, searchTypeId } = this.state;
    const isLoading = !filterPreview.result || !filterPreview.result.forId(queryId);
    let searchResult;
    let errors;

    if (!isLoading) {
      searchResult = filterPreview.result.forId(queryId).searchTypes[searchTypeId];
      // eslint-disable-next-line prefer-destructuring
      errors = filterPreview.result.errors; // result may not always be set, so I can't use destructuring
    }

    return (
      <FilterPreview eventDefinition={eventDefinition}
                     isFetchingData={isLoading}
                     displayPreview={this.isPermittedToSeePreview(currentUser, eventDefinition.config)}
                     searchResult={searchResult}
                     errors={errors} />
    );
  }
}

export default connect(FilterPreviewContainer, {
  filterPreview: FilterPreviewStore,
  currentUser: CurrentUserStore,
},
({ currentUser, ...otherProps }) => ({
  ...otherProps,
  currentUser: currentUser.currentUser,
}));

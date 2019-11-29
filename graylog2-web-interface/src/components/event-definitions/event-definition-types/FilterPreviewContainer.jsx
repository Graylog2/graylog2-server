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

const PREVIEW_PERMISSIONS = [
  'streams:read',
  'extendedsearch:create',
  'extendedsearch:use',
];

class FilterPreviewContainer extends React.Component {
  state = {
    queryId: uuid(),
    searchTypeId: uuid(),
  };

  fetchSearch = lodash.debounce((config) => {
    const { currentUser } = this.props;
    if (!PermissionsMixin.isPermitted(currentUser.permissions, PREVIEW_PERMISSIONS)) {
      return;
    }

    const { queryId, searchTypeId } = this.state;

    const formattedStreams = config.streams.map(stream => ({ type: 'stream', id: stream }));

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
      .parameters(config.query_parameters.filter(param => (!param.embryonic)))
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

    const isPermittedToSeePreview = PermissionsMixin.isPermitted(currentUser.permissions, PREVIEW_PERMISSIONS);

    return (
      <FilterPreview eventDefinition={eventDefinition}
                     isFetchingData={isLoading}
                     displayPreview={isPermittedToSeePreview}
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

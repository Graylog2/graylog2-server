import React from 'react';
import PropTypes from 'prop-types';
import lodash from 'lodash';
import uuid from 'uuid/v4';

import Query from 'views/logic/queries/Query';
import Search from 'views/logic/search/Search';
import CombinedProvider from 'injection/CombinedProvider';
import connect from 'stores/connect';

import FilterPreview from './FilterPreview';

const { FilterPreviewStore, FilterPreviewActions } = CombinedProvider.get('FilterPreview');

class FilterPreviewContainer extends React.Component {
  static propTypes = {
    eventDefinition: PropTypes.object.isRequired,
    filterPreview: PropTypes.object.isRequired,
  };

  state = {
    queryId: uuid(),
    searchTypeId: uuid(),
  };

  fetchSearch = lodash.debounce((config) => {
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
      .queries([query])
      .build();

    FilterPreviewActions.search(search);
  }, 250);

  componentDidMount() {
    const { eventDefinition } = this.props;
    this.fetchSearch(eventDefinition.config);
  }

  componentDidUpdate(prevProps) {
    const { eventDefinition } = this.props;

    if (!lodash.isEqual(prevProps.eventDefinition.config, eventDefinition.config)) {
      this.fetchSearch(eventDefinition.config);
    }
  }

  render() {
    const { eventDefinition, filterPreview } = this.props;
    const { queryId, searchTypeId } = this.state;
    const isLoading = !filterPreview.result || !filterPreview.result.forId(queryId);
    let searchResult;

    if (!isLoading) {
      searchResult = filterPreview.result.forId(queryId).searchTypes[searchTypeId];
    }

    return <FilterPreview eventDefinition={eventDefinition} isFetchingData={isLoading} searchResult={searchResult} />;
  }
}

export default connect(FilterPreviewContainer, {
  filterPreview: FilterPreviewStore,
});

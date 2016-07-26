import React, { PropTypes } from 'react';
import Reflux from 'reflux';
import Immutable from 'immutable';
import moment from 'moment';

import CombinedProvider from 'injection/CombinedProvider';
const { NodesStore, NodesActions } = CombinedProvider.get('Nodes');
const { CurrentUserStore } = CombinedProvider.get('CurrentUser');
const { InputsStore, InputsActions } = CombinedProvider.get('Inputs');
const { MessageFieldsStore } = CombinedProvider.get('MessageFields');
const { RefreshStore } = CombinedProvider.get('Refresh');
const { StreamsStore } = CombinedProvider.get('Streams');
const { UniversalSearchStore } = CombinedProvider.get('UniversalSearch');
const { SearchStore } = CombinedProvider.get('Search');
const { DecoratorsActions } = CombinedProvider.get('Decorators');

import { Spinner } from 'components/common';
import { MalformedSearchQuery, SearchResult } from 'components/search';

const SearchPage = React.createClass({
  propTypes: {
    location: PropTypes.object.isRequired,
    searchConfig: PropTypes.object.isRequired,
    searchInStream: PropTypes.object,
  },
  mixins: [
    Reflux.connect(NodesStore),
    Reflux.connect(MessageFieldsStore),
    Reflux.connect(CurrentUserStore),
    Reflux.listenTo(InputsStore, '_formatInputs'),
    Reflux.listenTo(RefreshStore, '_setupTimer', '_setupTimer'),
  ],
  getInitialState() {
    return {
      selectedFields: ['message', 'source'],
      error: undefined,
    };
  },
  componentDidMount() {
    [DecoratorsActions.create.completed, DecoratorsActions.remove.completed, DecoratorsActions.update.completed].forEach((action) => action.listen(this._refreshData));
    this._refreshData();
    InputsActions.list.triggerPromise();

    StreamsStore.listStreams().then((streams) => {
      const streamsMap = {};
      streams.forEach((stream) => { streamsMap[stream.id] = stream; });
      this.setState({ streams: Immutable.Map(streamsMap) });
    });

    NodesActions.list();
  },
  componentWillReceiveProps(nextProps) {
    const currentLocation = this.props.location || {};
    const nextLocation = nextProps.location || {};

    if ((currentLocation !== nextLocation) || (currentLocation.search !== nextLocation.search)) {
      this._refreshData();
    }
  },
  componentWillUnmount() {
    this._stopTimer();
  },
  _setupTimer(refresh) {
    this._stopTimer();
    if (refresh.enabled) {
      this.timer = setInterval(this._refreshData, refresh.interval);
    }
  },
  _stopTimer() {
    if (this.timer) {
      clearInterval(this.timer);
    }
  },
  _getEffectiveQuery() {
    return SearchStore.query.length > 0 ? SearchStore.query : '*';
  },
  _refreshData() {
    const query = this._getEffectiveQuery();
    const streamId = this.props.searchInStream ? this.props.searchInStream.id : undefined;
    UniversalSearchStore.search(SearchStore.rangeType, query, SearchStore.rangeParams.toJS(), streamId, null, SearchStore.page, SearchStore.sortField, SearchStore.sortOrder)
      .then(
        response => {
          if (this.isMounted()) {
            this.setState({ searchResult: response, error: undefined });
          }

          const interval = this.props.location.query.interval ? this.props.location.query.interval : this._determineHistogramResolution(response);

          UniversalSearchStore.histogram(SearchStore.rangeType, query, SearchStore.rangeParams.toJS(), interval, streamId).then((histogram) => {
            this.setState({ histogram: histogram });
          });
        },
        error => {
          // Treat searches with a malformed query
          if (error.additional && error.additional.status === 400) {
            this.setState({ error: error.additional.body });
          }
        }
      );
  },
  _formatInputs(state) {
    const inputs = InputsStore.inputsAsMap(state.inputs);
    this.setState({ inputs: Immutable.Map(inputs) });
  },
  _determineSearchDuration(response) {
    const searchTo = response.to;
    let searchFrom;
    if (SearchStore.rangeType === 'relative' && SearchStore.rangeParams.get('relative') === 0) {
      const sortedIndices = response.used_indices.sort((i1, i2) => moment(i1.end) - moment(i2.end));
      // If we didn't calculate index ranges for the oldest index, pick the next one.
      // This usually happens to the deflector, when index ranges weren't calculated for it yet.
      const oldestIndex = moment(sortedIndices[0].end).valueOf() === 0 ? sortedIndices[1] : sortedIndices[0];

      if (oldestIndex !== undefined) {
        searchFrom = oldestIndex.begin;
      } else {
        // We don't know when we received the first message, assume the search duration is 0.
        searchFrom = searchTo;
      }
    } else {
      searchFrom = response.from;
    }

    const queryRangeInMinutes = moment(searchTo).diff(searchFrom, 'minutes');

    return moment.duration(queryRangeInMinutes, 'minutes');
  },
  _determineHistogramResolution(response) {
    const duration = this._determineSearchDuration(response);

    if (duration.asHours() < 12) {
      return 'minute';
    }

    if (duration.asDays() < 3) {
      return 'hour';
    }

    if (duration.asDays() < 30) {
      return 'day';
    }

    if (duration.asMonths() < 2) {
      return 'week';
    }

    if (duration.asMonths() < 18) {
      return 'month';
    }

    if (duration.asYears() < 3) {
      return 'quarter';
    }

    return 'year';
  },
  sortFields(fieldSet) {
    let newFieldSet = fieldSet;
    let sortedFields = Immutable.OrderedSet();

    if (newFieldSet.contains('source')) {
      sortedFields = sortedFields.add('source');
    }
    newFieldSet = newFieldSet.delete('source');
    const remainingFieldsSorted = newFieldSet.sort((field1, field2) => field1.toLowerCase().localeCompare(field2.toLowerCase()));
    return sortedFields.concat(remainingFieldsSorted);
  },

  _onToggled(fieldName) {
    if (this.state.selectedFields.indexOf(fieldName) > 0) {
      this.setState({ selectedFields: this.state.selectedFields.filter((field) => field !== fieldName) });
    } else {
      this.setState({ selectedFields: this.state.selectedFields.concat(fieldName) });
    }
  },

  _isLoading() {
    return !this.state.searchResult || !this.state.inputs || !this.state.streams || !this.state.nodes || !this.state.fields || !this.state.histogram;
  },

  render() {
    if (this.state.error) {
      return <MalformedSearchQuery error={this.state.error} />;
    }

    if (this._isLoading()) {
      return <Spinner />;
    }

    const searchResult = this.state.searchResult;
    searchResult.all_fields = this.state.fields;
    return (
      <SearchResult query={SearchStore.query} page={SearchStore.page} builtQuery={searchResult.built_query}
                    result={searchResult} histogram={this.state.histogram}
                    formattedHistogram={this.state.histogram.histogram}
                    streams={this.state.streams} inputs={this.state.inputs} nodes={Immutable.Map(this.state.nodes)}
                    searchInStream={this.props.searchInStream} permissions={this.state.currentUser.permissions}
                    searchConfig={this.props.searchConfig} />
    );
  },
});

export default SearchPage;

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
const { DecoratorsStore } = CombinedProvider.get('Decorators');

import { DocumentTitle, Spinner } from 'components/common';
import { MalformedSearchQuery, SearchExecutionError, SearchResult } from 'components/search';

const SearchPage = React.createClass({
  propTypes: {
    location: PropTypes.object.isRequired,
    searchConfig: PropTypes.object.isRequired,
    searchInStream: PropTypes.object,
    forceFetch: PropTypes.bool,
  },
  mixins: [
    Reflux.connect(NodesStore),
    Reflux.connect(MessageFieldsStore),
    Reflux.connect(CurrentUserStore),
    Reflux.listenTo(InputsStore, '_formatInputs'),
    Reflux.listenTo(RefreshStore, '_setupTimer', '_setupTimer'),
    Reflux.listenTo(DecoratorsStore, '_refreshDataFromDecoratorStore', '_refreshDataFromDecoratorStore'),
  ],
  getInitialState() {
    return {
      error: undefined,
      updatingSearch: false,
      updatingHistogram: false,
    };
  },
  componentDidMount() {
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

    if (currentLocation.search !== nextLocation.search || this.props.searchInStream !== nextProps.searchInStream || nextProps.forceFetch) {
      if (this.promise) {
        this.promise.cancel();
      }
      this._refreshData(nextProps.searchInStream);
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
  _refreshDataFromDecoratorStore() {
    const searchInStream = this.props.searchInStream;
    this._refreshData(searchInStream);
  },
  _refreshData(searchInStream) {
    const query = SearchStore.originalQuery;
    const stream = searchInStream || this.props.searchInStream || {};
    const streamId = stream.id;
    if (this.promise && !this.promise.isCancelled()) {
      return this.promise;
    }
    if (!RefreshStore.enabled || RefreshStore.enabled && parseInt(RefreshStore.interval) > 5000) {
      this.setState({ updatingSearch: true });
    }
    this.promise = UniversalSearchStore.search(SearchStore.originalRangeType, query, SearchStore.originalRangeParams.toJS(), streamId, null, SearchStore.page, SearchStore.sortField, SearchStore.sortOrder)
      .then(
        (response) => {
          if (this.isMounted()) {
            this.setState({ searchResult: response, error: undefined });
          }

          const interval = this.props.location.query.interval ? this.props.location.query.interval : this._determineHistogramResolution(response);

          if (!RefreshStore.enabled || RefreshStore.enabled && parseInt(RefreshStore.interval) > 5000) {
             this.setState({ updatingHistogram: true });
          }
          UniversalSearchStore.histogram(SearchStore.originalRangeType, query, SearchStore.originalRangeParams.toJS(), interval, streamId)
            .then((histogram) => {
              this.setState({ histogram: histogram });
              return histogram;
            })
            .finally(() => this.setState({ updatingHistogram: false }));

          return response;
        },
        (error) => {
          // Treat searches with a malformed query
          if (error.additional) {
            if (error.additional.status) {
              this.setState({ error: error.additional });
            }
          }
        },
      )
      .finally(() => {
        this.setState({ updatingSearch: false });
        this.promise = undefined;
      });
  },
  _formatInputs(state) {
    const inputs = InputsStore.inputsAsMap(state.inputs);
    this.setState({ inputs: Immutable.Map(inputs) });
  },
  _determineSearchDuration(response) {
    const searchTo = response.to;
    let searchFrom;
    if (SearchStore.originalRangeType === 'relative' && SearchStore.originalRangeParams.get('relative') === 0) {
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

  _isLoading() {
    return !this.state.searchResult || !this.state.inputs || !this.state.streams || !this.state.nodes || !this.state.fields || !this.state.histogram;
  },

  render() {
    if (this.state.error) {
      let errorPage;
      switch (this.state.error.status) {
        case 400:
          errorPage = <MalformedSearchQuery error={this.state.error} />;
          break;
        default:
          errorPage = <SearchExecutionError error={this.state.error} />;
      }

      return <DocumentTitle title="Search error">{errorPage}</DocumentTitle>;
    }

    if (this._isLoading()) {
      return <Spinner />;
    }

    const searchResult = this.state.searchResult;
    searchResult.all_fields = this.state.fields;
    return (
      <DocumentTitle title="Search">
        <SearchResult query={SearchStore.originalQuery} page={SearchStore.page} builtQuery={searchResult.built_query}
                      result={searchResult} histogram={this.state.histogram}
                      formattedHistogram={this.state.histogram.histogram}
                      streams={this.state.streams} inputs={this.state.inputs} nodes={Immutable.Map(this.state.nodes)}
                      searchInStream={this.props.searchInStream} permissions={this.state.currentUser.permissions}
                      searchConfig={this.props.searchConfig}
                      loadingSearch={this.state.updatingSearch || this.state.updatingHistogram}
                      forceFetch={this.props.forceFetch} />
      </DocumentTitle>
    );
  },
});

export default SearchPage;

import React, {PropTypes} from 'react';
import Reflux from 'reflux';
import Immutable from 'immutable';
import moment from 'moment';

import StoreProvider from 'injection/StoreProvider';
const NodesStore = StoreProvider.getStore('Nodes');
const CurrentUserStore = StoreProvider.getStore('CurrentUser');
const InputsStore = StoreProvider.getStore('Inputs');
const MessageFieldsStore = StoreProvider.getStore('MessageFields');
const RefreshStore = StoreProvider.getStore('Refresh');
const StreamsStore = StoreProvider.getStore('Streams');
const UniversalSearchStore = StoreProvider.getStore('UniversalSearch');
const SearchStore = StoreProvider.getStore('Search');

import ActionsProvider from 'injection/ActionsProvider';
const NodesActions = ActionsProvider.getActions('Nodes');
const InputsActions = ActionsProvider.getActions('Inputs');

import { Spinner } from 'components/common';
import { SearchResult } from 'components/search';

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
      query: SearchStore.query.length > 0 ? SearchStore.query : '*',
    };
  },
  componentDidMount() {
    this._refreshData();
    InputsActions.list.triggerPromise();

    StreamsStore.listStreams().then((streams) => {
      const streamsMap = {};
      streams.forEach((stream) => streamsMap[stream.id] = stream);
      this.setState({streams: Immutable.Map(streamsMap)});
    });

    NodesActions.list();
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
  _refreshData() {
    const query = this.state.query;
    const streamId = this.props.searchInStream ? this.props.searchInStream.id : undefined;
    UniversalSearchStore.search(SearchStore.rangeType, query, SearchStore.rangeParams.toJS(), streamId, null, SearchStore.page).then((response) => {
      this.setState({ searchResult: response });

      const interval = this.props.location.query.interval ? this.props.location.query.interval : this._determineHistogramResolution(response);

      UniversalSearchStore.histogram(SearchStore.rangeType, query, SearchStore.rangeParams.toJS(), interval, streamId).then((histogram) => {
        this.setState({histogram: histogram});
      });
    });
  },
  _formatInputs(state) {
    const inputs = InputsStore.inputsAsMap(state.inputs);
    this.setState({inputs: Immutable.Map(inputs)});
  },
  _determineHistogramResolution(response) {
    let queryRangeInMinutes;
    if (SearchStore.rangeType === 'relative' && SearchStore.rangeParams.get('relative') === 0) {
      const oldestIndex = response.used_indices.sort((i1, i2) => moment(i2.end) - moment(i1.end))[0];
      queryRangeInMinutes = moment(response.to).diff(oldestIndex.begin, 'minutes');
    } else {
      queryRangeInMinutes = moment(response.to).diff(response.from, 'minutes');
    }

    const duration = moment.duration(queryRangeInMinutes, 'minutes');

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
      this.setState({selectedFields: this.state.selectedFields.filter((field) => field !== fieldName)});
    } else {
      this.setState({selectedFields: this.state.selectedFields.concat(fieldName)});
    }
  },

  render() {
    if (!this.state.searchResult || !this.state.inputs || !this.state.streams || !this.state.nodes || !this.state.fields || !this.state.histogram) {
      return <Spinner />;
    }
    const searchResult = this.state.searchResult;
    searchResult.all_fields = this.state.fields;
    const selectedFields = this.sortFields(Immutable.List(this.state.selectedFields));
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

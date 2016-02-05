import React, {PropTypes} from 'react';
import Reflux from 'reflux';
import Immutable from 'immutable';
import moment from 'moment';

import CurrentUserStore from 'stores/users/CurrentUserStore';
import DashboardsStore from 'stores/dashboards/DashboardsStore';
import InputsActions from 'actions/inputs/InputsActions';
import InputsStore from 'stores/inputs/InputsStore';
import MessageFieldsStore from 'stores/messages/MessageFieldsStore';
import NodesStore from 'stores/nodes/NodesStore';
import StreamsStore from 'stores/streams/StreamsStore';
import UniversalSearchStore from 'stores/search/UniversalSearchStore';

import SearchStore from 'stores/search/SearchStore';

import NodesActions from 'actions/nodes/NodesActions';

import { Spinner } from 'components/common';
import { SearchResult } from 'components/search';

const SearchPage = React.createClass({
  propTypes: {
    location: PropTypes.object.isRequired,
    searchInStream: PropTypes.object,
  },
  getInitialState() {
    return {
      selectedFields: ['message', 'source'],
    };
  },
  mixins: [Reflux.connect(NodesStore), Reflux.connect(MessageFieldsStore), Reflux.connect(CurrentUserStore), Reflux.listenTo(InputsStore, '_formatInputs')],
  componentDidMount() {
    const query = SearchStore.query.length > 0 ? SearchStore.query : '*';
    const streamId = this.props.searchInStream ? this.props.searchInStream.id : undefined;
    UniversalSearchStore.search(SearchStore.rangeType, query, SearchStore.rangeParams.toJS(), streamId).then((response) => {
      this.setState({searchResult: response});

      const interval = this.props.location.query.interval ? this.props.location.query.interval : this._determineHistogramResolution(response);

      UniversalSearchStore.histogram(SearchStore.rangeType, query, SearchStore.rangeParams.toJS(), interval, streamId).then((histogram) => {
        this.setState({histogram: histogram});
      });
    });
    InputsActions.list.triggerPromise();

    StreamsStore.listStreams().then((streams) => {
      const streamsMap = {};
      streams.forEach((stream) => streamsMap[stream.id] = stream);
      this.setState({streams: Immutable.Map(streamsMap)});
    });

    NodesActions.list();
    DashboardsStore.updateWritableDashboards();
  },
  _formatInputs(state) {
    const inputs = InputsStore.inputsAsMap(state.inputs);
    this.setState({inputs: Immutable.Map(inputs)});
  },
  _determineHistogramResolution(response) {
    let queryRangeInMinutes;
    if (SearchStore.rangeType === 'relative' && SearchStore.rangeParams.get('relative') === 0) {
      const oldestIndex = Object.keys(response.used_indices)
        .map((key) => response.used_indices[key])
        .sort((i1, i2) => moment(i1.end).isAfter(i2.end))[0];
      queryRangeInMinutes = moment(response.to).diff(oldestIndex.begin, 'minutes');
    } else {
      queryRangeInMinutes = moment(response.to).diff(response.from, 'minutes');
    }

    const duration = moment.duration(queryRangeInMinutes, 'minutes');

    if (duration.hours() < 12) {
      return 'minute';
    }

    if (duration.days() < 2) {
      return 'hour';
    }

    if (duration.days() < 30) {
      return 'day';
    }

    if (duration.days() < 6 * 30) {
      return 'week';
    }

    if (duration.days() < 2 * 365) {
      return 'month';
    }

    if (duration.days() < 10 * 365) {
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
      <SearchResult query={SearchStore.query} builtQuery={searchResult.built_query}
                    result={searchResult} histogram={this.state.histogram}
                    formattedHistogram={this.state.histogram.histogram}
                    streams={this.state.streams} inputs={this.state.inputs} nodes={Immutable.Map(this.state.nodes)}
                    searchInStream={this.props.searchInStream} permissions={this.state.currentUser.permissions} />
    );
  },
});

export default SearchPage;

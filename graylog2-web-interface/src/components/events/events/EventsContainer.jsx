import React from 'react';
import PropTypes from 'prop-types';

import { Spinner } from 'components/common';

import connect from 'stores/connect';
import CombinedProvider from 'injection/CombinedProvider';

import Events from './Events';

import {} from 'components/event-definitions/event-definition-types';

const { EventsStore, EventsActions } = CombinedProvider.get('Events');

class EventsContainer extends React.Component {
  static propTypes = {
    events: PropTypes.object.isRequired,
  };

  componentDidMount() {
    this.fetchData({});
  }

  fetchData = ({ page, pageSize, query, filter, timerange }) => {
    return EventsActions.search({
      query: query,
      page: page,
      pageSize: pageSize,
      filter: filter,
      timerange: timerange,
    });
  };

  handlePageChange = (nextPage, nextPageSize) => {
    const { events } = this.props;
    this.fetchData({
      page: nextPage,
      pageSize: nextPageSize,
      query: events.parameters.query,
      filter: events.parameters.filter,
      timerange: events.parameters.timerange,
    });
  };

  handleQueryChange = (nextQuery, callback = () => {}) => {
    const { events } = this.props;
    const promise = this.fetchData({
      query: nextQuery,
      pageSize: events.parameters.pageSize,
      filter: events.parameters.filter,
      timerange: events.parameters.timerange,
    });
    promise.finally(callback);
  };

  handleAlertFilterChange = (nextAlertFilter) => {
    return () => {
      const { events } = this.props;
      this.fetchData({
        query: events.parameters.query,
        pageSize: events.parameters.pageSize,
        filter: { alerts: nextAlertFilter },
        timerange: events.parameters.timerange,
      });
    };
  };

  handleTimeRangeChange = (timeRangeType, range) => {
    const { events } = this.props;
    this.fetchData({
      query: events.parameters.query,
      pageSize: events.parameters.pageSize,
      filter: events.parameters.filter,
      timerange: { type: timeRangeType, range: range },
    });
  };

  render() {
    const { events } = this.props;

    if (!events.events) {
      return <Spinner text="Loading Events information..." />;
    }

    return (
      <Events events={events.events}
              parameters={events.parameters}
              totalEvents={events.totalEvents}
              context={events.context}
              onQueryChange={this.handleQueryChange}
              onPageChange={this.handlePageChange}
              onAlertFilterChange={this.handleAlertFilterChange}
              onTimeRangeChange={this.handleTimeRangeChange} />
    );
  }
}

export default connect(EventsContainer, { events: EventsStore });

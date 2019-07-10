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

  fetchData = ({ page, pageSize, query }) => {
    return EventsActions.search({
      query: query,
      page: page,
      pageSize: pageSize,
    });
  };

  handlePageChange = (nextPage, nextPageSize) => {
    const { events } = this.props;
    this.fetchData({ page: nextPage, pageSize: nextPageSize, query: events.parameters.query });
  };

  handleQueryChange = (nextQuery, callback = () => {}) => {
    const { events } = this.props;
    const promise = this.fetchData({ query: nextQuery, pageSize: events.parameters.pageSize });
    promise.finally(callback);
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
              onPageChange={this.handlePageChange} />
    );
  }
}

export default connect(EventsContainer, { events: EventsStore });

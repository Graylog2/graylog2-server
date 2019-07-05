import React from 'react';
import PropTypes from 'prop-types';

import { Spinner } from 'components/common';

import connect from 'stores/connect';
import CombinedProvider from 'injection/CombinedProvider';

import {} from 'components/event-definitions/event-definition-types';

import EventDefinitions from './EventDefinitions';

const { EventDefinitionsStore, EventDefinitionsActions } = CombinedProvider.get('EventDefinitions');

class EventDefinitionsContainer extends React.Component {
  static propTypes = {
    eventDefinitions: PropTypes.object.isRequired,
  };

  componentDidMount() {
    this.fetchData({});
  }

  fetchData = ({ page, pageSize, query }) => {
    return EventDefinitionsActions.listPaginated({
      query: query,
      page: page,
      pageSize: pageSize,
    });
  };

  handlePageChange = (nextPage, nextPageSize) => {
    const { eventDefinitions } = this.props;
    this.fetchData({ page: nextPage, pageSize: nextPageSize, query: eventDefinitions.query });
  };

  handleQueryChange = (nextQuery, callback = () => {}) => {
    const { eventDefinitions } = this.props;
    const promise = this.fetchData({ query: nextQuery, pageSize: eventDefinitions.pagination.pageSize });
    promise.finally(callback);
  };

  handleDelete = (definition) => {
    return () => {
      if (window.confirm(`Are you sure you want to delete "${definition.title}"?`)) {
        EventDefinitionsActions.delete(definition);
      }
    };
  };

  render() {
    const { eventDefinitions } = this.props;

    if (!eventDefinitions.eventDefinitions) {
      return <Spinner text="Loading Event Definitions information..." />;
    }

    return (
      <EventDefinitions eventDefinitions={eventDefinitions.eventDefinitions}
                        pagination={eventDefinitions.pagination}
                        query={eventDefinitions.query}
                        onPageChange={this.handlePageChange}
                        onQueryChange={this.handleQueryChange}
                        onDelete={this.handleDelete} />
    );
  }
}

export default connect(EventDefinitionsContainer, { eventDefinitions: EventDefinitionsStore });

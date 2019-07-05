import React from 'react';
import PropTypes from 'prop-types';

import { Spinner } from 'components/common';

import connect from 'stores/connect';
import CombinedProvider from 'injection/CombinedProvider';
import EventDefinitions from './EventDefinitions';

const { EventDefinitionsStore, EventDefinitionsActions } = CombinedProvider.get('EventDefinitions');

class EventDefinitionsContainer extends React.Component {
  static propTypes = {
    eventDefinitions: PropTypes.object.isRequired,
  };

  componentDidMount() {
    this.fetchData();
  }

  fetchData = (nextPage, nextPageSize) => {
    EventDefinitionsActions.listPaginated({ page: nextPage, pageSize: nextPageSize });
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
      <EventDefinitions eventDefinitions={eventDefinitions.eventDefinitions} onDelete={this.handleDelete} />
    );
  }
}

export default connect(EventDefinitionsContainer, { eventDefinitions: EventDefinitionsStore });

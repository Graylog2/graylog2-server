import React from 'react';
import PropTypes from 'prop-types';

import { Spinner } from 'components/common';

import connect from 'stores/connect';
import CombinedProvider from 'injection/CombinedProvider';
import EventNotifications from './EventNotifications';

// Import built-in Event Notification Types
import {} from '../event-notification-types';

const { EventNotificationsStore, EventNotificationsActions } = CombinedProvider.get('EventNotifications');

class EventNotificationsContainer extends React.Component {
  static propTypes = {
    notifications: PropTypes.object,
  };

  static defaultProps = {
    notifications: undefined,
  };

  componentDidMount() {
    this.fetchData();
  }

  fetchData = (nextPage, nextPageSize) => {
    EventNotificationsActions.listPaginated({ page: nextPage, pageSize: nextPageSize });
  };

  handlePageChange = (nextPage, nextPageSize) => {
    this.fetchData(nextPage, nextPageSize);
  };

  handleDelete = (definition) => {
    return () => {
      if (window.confirm(`Are you sure you want to delete "${definition.title}"?`)) {
        EventNotificationsActions.delete(definition);
      }
    };
  };

  render() {
    const { notifications } = this.props;

    if (!notifications) {
      return <Spinner text="Loading Notifications information..." />;
    }

    return (
      <EventNotifications notifications={notifications.notifications}
                          pagination={notifications.pagination}
                          onPageChange={this.handlePageChange}
                          onDelete={this.handleDelete} />
    );
  }
}

export default connect(EventNotificationsContainer, { notifications: EventNotificationsStore });

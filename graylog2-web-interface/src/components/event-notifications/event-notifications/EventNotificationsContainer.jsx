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
    this.fetchData({});
  }

  fetchData = ({ page, pageSize, query }) => {
    return EventNotificationsActions.listPaginated({
      query: query,
      page: page,
      pageSize: pageSize,
    });
  };

  handlePageChange = (nextPage, nextPageSize) => {
    const { notifications } = this.props;
    this.fetchData({ page: nextPage, pageSize: nextPageSize, query: notifications.query });
  };

  handleQueryChange = (nextQuery, callback = () => {}) => {
    const { notifications } = this.props;
    const promise = this.fetchData({ query: nextQuery, pageSize: notifications.pagination.pageSize });
    promise.finally(callback);
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

    if (!notifications.notifications) {
      return <Spinner text="Loading Notifications information..." />;
    }

    return (
      <EventNotifications notifications={notifications.notifications}
                          pagination={notifications.pagination}
                          query={notifications.query}
                          onPageChange={this.handlePageChange}
                          onQueryChange={this.handleQueryChange}
                          onDelete={this.handleDelete} />
    );
  }
}

export default connect(EventNotificationsContainer, { notifications: EventNotificationsStore });

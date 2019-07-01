import React from 'react';
import PropTypes from 'prop-types';

import connect from 'stores/connect';
import CombinedProvider from 'injection/CombinedProvider';
import EventNotifications from './EventNotifications';

const { EventNotificationsStore, EventNotificationsActions } = CombinedProvider.get('EventNotifications');

class EventNotificationsContainer extends React.Component {
  static propTypes = {
    notifications: PropTypes.object.isRequired,
  };

  componentDidMount() {
    EventNotificationsActions.list();
  }

  handleDelete = (definition) => {
    return () => {
      if (window.confirm(`Are you sure you want to delete "${definition.title}"?`)) {
        EventNotificationsActions.delete(definition);
      }
    };
  };

  render() {
    const { notifications } = this.props;
    return (
      <EventNotifications notifications={notifications} onDelete={this.handleDelete} />
    );
  }
}

export default connect(EventNotificationsContainer, { notifications: EventNotificationsStore });

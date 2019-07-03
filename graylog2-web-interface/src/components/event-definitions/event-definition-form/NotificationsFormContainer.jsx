import React from 'react';
import PropTypes from 'prop-types';

import { Spinner } from 'components/common';

import connect from 'stores/connect';
import CombinedProvider from 'injection/CombinedProvider';

import NotificationsForm from './NotificationsForm';

const { EventNotificationsStore, EventNotificationsActions } = CombinedProvider.get('EventNotifications');

class NotificationsFormContainer extends React.Component {
  static propTypes = {
    eventDefinition: PropTypes.object.isRequired,
    notifications: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
  };

  componentDidMount() {
    this.fetchNotifications();
  }

  fetchNotifications = () => {
    EventNotificationsActions.listAll();
  };

  render() {
    const { notifications, ...otherProps } = this.props;

    if (!notifications.all) {
      return <Spinner text="Loading Notification information..." />;
    }

    return <NotificationsForm {...otherProps} notifications={notifications.all} />;
  }
}

export default connect(NotificationsFormContainer, { notifications: EventNotificationsStore });

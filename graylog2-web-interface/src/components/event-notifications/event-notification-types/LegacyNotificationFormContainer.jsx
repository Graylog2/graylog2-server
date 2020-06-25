import React from 'react';
import PropTypes from 'prop-types';

import { Spinner } from 'components/common';
import CombinedProvider from 'injection/CombinedProvider';
import connect from 'stores/connect';

import LegacyNotificationForm from './LegacyNotificationForm';

const { EventNotificationsStore, EventNotificationsActions } = CombinedProvider.get('EventNotifications');

class LegacyNotificationFormContainer extends React.Component {
  static propTypes = {
    config: PropTypes.object.isRequired,
    validation: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
    notifications: PropTypes.object.isRequired,
  };

  componentDidMount() {
    const { notifications } = this.props;

    if (!notifications.allLegacyTypes) {
      EventNotificationsActions.listAllLegacyTypes();
    }
  }

  render() {
    const { notifications } = this.props;
    const { allLegacyTypes } = notifications;

    if (!allLegacyTypes) {
      return <p><Spinner text="Loading legacy notification information..." /></p>;
    }

    return <LegacyNotificationForm {...this.props} legacyTypes={allLegacyTypes} />;
  }
}

export default connect(LegacyNotificationFormContainer, {
  notifications: EventNotificationsStore,
});

import React from 'react';
import PropTypes from 'prop-types';

import { Spinner } from 'components/common';
import CombinedProvider from 'injection/CombinedProvider';
import connect from 'stores/connect';

import LegacyNotificationSummary from './LegacyNotificationSummary';

const { EventNotificationsStore, EventNotificationsActions } = CombinedProvider.get('EventNotifications');

class LegacyNotificationSummaryContainer extends React.Component {
  static propTypes = {
    type: PropTypes.string.isRequired,
    notification: PropTypes.object,
    notifications: PropTypes.object.isRequired,
    definitionNotification: PropTypes.object.isRequired,
  };

  static defaultProps = {
    notification: {},
  };

  componentDidMount() {
    EventNotificationsActions.listAllLegacyTypes();
  }

  render() {
    const { notifications } = this.props;
    const { allLegacyTypes } = notifications;

    if (!allLegacyTypes) {
      return <p><Spinner text="Loading legacy notification information..." /></p>;
    }

    return <LegacyNotificationSummary {...this.props} legacyTypes={allLegacyTypes} />;
  }
}

export default connect(LegacyNotificationSummaryContainer, {
  notifications: EventNotificationsStore,
});

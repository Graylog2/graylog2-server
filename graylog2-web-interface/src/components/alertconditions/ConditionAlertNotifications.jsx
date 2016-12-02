import React from 'react';

import { Spinner } from 'components/common';
import { AlertNotificationsList } from 'components/alertnotifications';

import CombinedProvider from 'injection/CombinedProvider';
const { AlarmCallbacksActions } = CombinedProvider.get('AlarmCallbacks');
const { AlertNotificationsActions } = CombinedProvider.get('AlertNotifications');

const ConditionAlertNotifications = React.createClass({
  propTypes: {
    alertCondition: React.PropTypes.object.isRequired,
    stream: React.PropTypes.object.isRequired,
  },

  getInitialState() {
    return {
      conditionNotifications: undefined,
    };
  },

  componentDidMount() {
    AlertNotificationsActions.available();
    AlarmCallbacksActions.list(this.props.stream.id)
      .then(callbacks => this.setState({ conditionNotifications: callbacks }));
  },

  _isLoading() {
    return !this.state.conditionNotifications;
  },

  render() {
    if (this._isLoading()) {
      return <Spinner />;
    }

    const notifications = this.state.conditionNotifications;

    return (
      <div>
        <h2>Notifications</h2>
        <p>Define the notifications to send when the condition is satisfied.</p>

        <AlertNotificationsList alertNotifications={notifications} streams={[this.props.stream]} />
      </div>
    );
  },
});

export default ConditionAlertNotifications;

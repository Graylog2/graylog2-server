import PropTypes from 'prop-types';
import React from 'react';
import naturalSort from 'javascript-natural-sort';

import { Pluralize, Spinner } from 'components/common';
import { AlertNotificationsList } from 'components/alertnotifications';

import CombinedProvider from 'injection/CombinedProvider';

const { AlarmCallbacksActions } = CombinedProvider.get('AlarmCallbacks');
const { AlertNotificationsActions } = CombinedProvider.get('AlertNotifications');

class StreamAlertNotifications extends React.Component {
  static propTypes = {
    stream: PropTypes.object.isRequired,
  };

  state = {
    conditionNotifications: undefined,
  };

  componentDidMount() {
    this._loadData();
  }

  _loadData = () => {
    AlertNotificationsActions.available();
    AlarmCallbacksActions.list(this.props.stream.id)
      .then(callbacks => this.setState({ conditionNotifications: callbacks }));
  };

  _isLoading = () => {
    return !this.state.conditionNotifications;
  };

  render() {
    if (this._isLoading()) {
      return <Spinner />;
    }

    const stream = this.props.stream;

    const notifications = this.state.conditionNotifications.sort((a1, a2) => {
      const t1 = a1.title || 'Untitled';
      const t2 = a2.title || 'Untitled';
      return naturalSort(t1.toLowerCase(), t2.toLowerCase());
    });

    return (
      <div>
        <h2>Notifications</h2>
        <p>
          <Pluralize value={notifications.length} singular="This is the notification" plural="These are the notifications" />
          {' '}set for the stream <em>{stream.title}</em>. <Pluralize value={notifications.length} singular="It" plural="They" />
          {' '}will be triggered when the alert condition is satisfied.
        </p>

        <AlertNotificationsList alertNotifications={notifications}
                                streams={[this.props.stream]}
                                onNotificationUpdate={this._loadData}
                                onNotificationDelete={this._loadData} />
      </div>
    );
  }
}

export default StreamAlertNotifications;

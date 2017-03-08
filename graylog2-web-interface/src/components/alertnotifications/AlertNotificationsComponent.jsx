import React from 'react';
import Reflux from 'reflux';
import { Button } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';
import naturalSort from 'javascript-natural-sort';

import { Spinner } from 'components/common';
import { AlertNotificationsList } from 'components/alertnotifications';

import Routes from 'routing/Routes';

import CombinedProvider from 'injection/CombinedProvider';
const { AlertNotificationsStore, AlertNotificationsActions } = CombinedProvider.get('AlertNotifications');
const { StreamsStore } = CombinedProvider.get('Streams');

const AlertNotificationsComponent = React.createClass({
  mixins: [Reflux.connect(AlertNotificationsStore)],

  getInitialState() {
    return {
      streams: undefined,
    };
  },

  componentDidMount() {
    this._loadData();
  },

  _loadData() {
    StreamsStore.listStreams().then((streams) => {
      this.setState({ streams: streams });
    });

    AlertNotificationsActions.available();
    AlertNotificationsActions.listAll();
  },

  _isLoading() {
    return !this.state.streams || !this.state.availableNotifications || !this.state.allNotifications;
  },

  render() {
    if (this._isLoading()) {
      return <Spinner />;
    }

    const notifications = this.state.allNotifications.sort((a1, a2) => {
      const t1 = a1.title || 'Untitled';
      const t2 = a2.title || 'Untitled';
      return naturalSort(t1.toLowerCase(), t2.toLowerCase());
    });

    return (
      <div>
        <div className="pull-right">
          <LinkContainer to={Routes.ALERTS.NEW_NOTIFICATION}>
            <Button bsStyle="success">Add new notification</Button>
          </LinkContainer>
        </div>
        <h2>Notifications</h2>
        <p>These are all configured alert notifications.</p>
        <AlertNotificationsList alertNotifications={notifications} streams={this.state.streams}
                                onNotificationUpdate={this._loadData} onNotificationDelete={this._loadData} />
      </div>
    );
  },
});

export default AlertNotificationsComponent;

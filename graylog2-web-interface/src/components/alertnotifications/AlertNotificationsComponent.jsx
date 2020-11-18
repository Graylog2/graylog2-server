/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';
import naturalSort from 'javascript-natural-sort';

import { LinkContainer } from 'components/graylog/router';
import { Button } from 'components/graylog';
import { Spinner } from 'components/common';
import { AlertNotificationsList } from 'components/alertnotifications';
import Routes from 'routing/Routes';
import CombinedProvider from 'injection/CombinedProvider';

const { AlertNotificationsStore, AlertNotificationsActions } = CombinedProvider.get('AlertNotifications');
const { StreamsStore } = CombinedProvider.get('Streams');

const AlertNotificationsComponent = createReactClass({
  displayName: 'AlertNotificationsComponent',
  mixins: [Reflux.connect(AlertNotificationsStore)],

  getInitialState() {
    return {
      allNotifications: undefined,
      streams: undefined,
    };
  },

  componentDidMount() {
    this._loadData();
  },

  _loadData() {
    StreamsStore.listStreams().then((streams) => {
      this.setState({ streams });
    });

    AlertNotificationsActions.available();
    AlertNotificationsActions.listAll();
  },

  _isLoading() {
    const { streams, allNotifications } = this.state;

    return !streams || !allNotifications;
  },

  render() {
    if (this._isLoading()) {
      return <Spinner />;
    }

    const { allNotifications, streams } = this.state;

    const notifications = allNotifications.sort((a1, a2) => {
      const t1 = a1.title || 'Untitled';
      const t2 = a2.title || 'Untitled';

      return naturalSort(t1.toLowerCase(), t2.toLowerCase());
    });

    return (
      <div>
        <div className="pull-right">
          <LinkContainer to={Routes.LEGACY_ALERTS.NEW_NOTIFICATION}>
            <Button bsStyle="success">Add new notification</Button>
          </LinkContainer>
        </div>
        <h2>Notifications</h2>
        <p>These are all configured alert notifications.</p>
        <AlertNotificationsList alertNotifications={notifications}
                                streams={streams}
                                onNotificationUpdate={this._loadData}
                                onNotificationDelete={this._loadData} />
      </div>
    );
  },
});

export default AlertNotificationsComponent;

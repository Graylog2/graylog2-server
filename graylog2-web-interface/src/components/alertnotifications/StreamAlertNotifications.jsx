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
import PropTypes from 'prop-types';
import React from 'react';
import naturalSort from 'javascript-natural-sort';

import { LinkContainer } from 'components/graylog/router';
import { Button } from 'components/graylog';
import { Spinner } from 'components/common';
import { AlertNotificationsList } from 'components/alertnotifications';
import CombinedProvider from 'injection/CombinedProvider';
import Routes from 'routing/Routes';

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
      .then((callbacks) => this.setState({ conditionNotifications: callbacks }));
  };

  _isLoading = () => {
    return !this.state.conditionNotifications;
  };

  render() {
    if (this._isLoading()) {
      return <Spinner />;
    }

    const { stream } = this.props;

    const notifications = this.state.conditionNotifications.sort((a1, a2) => {
      const t1 = a1.title || 'Untitled';
      const t2 = a2.title || 'Untitled';

      return naturalSort(t1.toLowerCase(), t2.toLowerCase());
    });

    return (
      <div>
        <div className="pull-right">
          <LinkContainer to={Routes.new_alert_notification_for_stream(stream.id)}>
            <Button bsStyle="success">Add new notification</Button>
          </LinkContainer>
        </div>
        <h2>Notifications</h2>
        <p className="description">
          Alert Notifications will be executed when a Condition belonging to this Stream is satisfied.
        </p>

        <AlertNotificationsList alertNotifications={notifications}
                                streams={[this.props.stream]}
                                onNotificationUpdate={this._loadData}
                                onNotificationDelete={this._loadData}
                                isStreamView />
      </div>
    );
  }
}

export default StreamAlertNotifications;

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

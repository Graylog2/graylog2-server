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
import * as React from 'react';
import PropTypes from 'prop-types';

import { LinkContainer } from 'components/graylog/router';
import { IfPermitted } from 'components/common';
import { Button, ButtonToolbar } from 'components/graylog';
import Routes from 'routing/Routes';

const EventNotificationActionLinks = ({ notificationId }) => (
  <ButtonToolbar>
    <IfPermitted permissions={`eventnotifications:read:${notificationId}`}>
      <LinkContainer to={Routes.ALERTS.NOTIFICATIONS.show(notificationId)}>
        <Button bsStyle="success">View Details</Button>
      </LinkContainer>
    </IfPermitted>
    <IfPermitted permissions={`eventnotifications:edit:${notificationId}`}>
      <LinkContainer to={Routes.ALERTS.NOTIFICATIONS.edit(notificationId)}>
        <Button bsStyle="success">Edit Notification</Button>
      </LinkContainer>
    </IfPermitted>
  </ButtonToolbar>
);

EventNotificationActionLinks.propTypes = {
  notificationId: PropTypes.string.isRequired,
};

export default EventNotificationActionLinks;

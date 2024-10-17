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

import { LinkContainer } from 'components/common/router';
import { Button, ButtonToolbar } from 'components/bootstrap';
import { IfPermitted } from 'components/common';
import Routes from 'routing/Routes';

type EventNotificationActionLinksProps = {
  notificationId: string;
};

const EventNotificationActionLinks = ({
  notificationId,
}: EventNotificationActionLinksProps) => (
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

export default EventNotificationActionLinks;

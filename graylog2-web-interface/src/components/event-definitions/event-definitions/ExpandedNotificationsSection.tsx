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
import styled, { css } from 'styled-components';

import { Alert, ListGroup, ListGroupItem } from 'components/bootstrap';
import { Spinner } from 'components/common';
import EventNotificationLink from 'components/event-notifications/event-notifications/EventNotificationLink';
import useResolvedNotifications from 'components/event-definitions/hooks/useResolvedNotifications';

import type { EventDefinition } from '../event-definitions-types';

const Description = styled.p(
  ({ theme }) => css`
    margin-bottom: ${theme.spacings.xs};
    color: ${theme.colors.gray[50]};
  `,
);

type Props = {
  eventDefinition: EventDefinition;
};

const ExpandedNotificationsSection = ({ eventDefinition }: Props) => {
  const { resolved, notPermittedIds, isLoading } = useResolvedNotifications(eventDefinition);

  const notificationList = resolved.map(({ id, title }) => (
    <ListGroupItem key={id}>
      <EventNotificationLink id={id} title={title} />
    </ListGroupItem>
  ));

  return (
    <>
      <Description>Notifications fired when this event definition triggers.</Description>
      {notPermittedIds.length > 0 && (
        <Alert bsStyle="warning">
          Missing Notifications Permissions for:
          <br />
          {notPermittedIds.join(', ')}
        </Alert>
      )}
      {isLoading ? (
        <Spinner text="Loading notifications..." />
      ) : (
        <ListGroup componentClass="ul">{notificationList}</ListGroup>
      )}
    </>
  );
};

export default ExpandedNotificationsSection;

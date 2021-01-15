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

import { LinkContainer } from 'components/graylog/router';
import { Button, ButtonToolbar, Col, Row } from 'components/graylog';
import { DocumentTitle, IfPermitted, PageHeader } from 'components/common';
import EventNotificationsContainer from 'components/event-notifications/event-notifications/EventNotificationsContainer';
import Routes from 'routing/Routes';

const EventNotificationsPage = () => {
  return (
    <DocumentTitle title="Notifications">
      <span>
        <PageHeader title="Notifications">
          <span>
            Notifications alert you of any configured Event when they occur. Graylog can send Notifications directly
            to you or to other systems you use for that purpose.
          </span>

          <span>
            Remember to assign Notifications while creating or editing an Event Definition.
          </span>

          <ButtonToolbar>
            <LinkContainer to={Routes.ALERTS.LIST}>
              <Button bsStyle="info">Alerts & Events</Button>
            </LinkContainer>
            <LinkContainer to={Routes.ALERTS.DEFINITIONS.LIST}>
              <Button bsStyle="info">Event Definitions</Button>
            </LinkContainer>
            <LinkContainer to={Routes.ALERTS.NOTIFICATIONS.LIST}>
              <Button bsStyle="info">Notifications</Button>
            </LinkContainer>
          </ButtonToolbar>
        </PageHeader>

        <Row className="content">
          <Col md={12}>
            <EventNotificationsContainer />
          </Col>
        </Row>
      </span>
    </DocumentTitle>
  );
};

export default EventNotificationsPage;

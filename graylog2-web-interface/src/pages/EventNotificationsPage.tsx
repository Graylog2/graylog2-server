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

import { LinkContainer } from 'components/common/router';
import { Button, Col, Row } from 'components/bootstrap';
import { DocumentTitle, IfPermitted, PageHeader } from 'components/common';
import EventNotificationsContainer from 'components/event-notifications/event-notifications/EventNotificationsContainer';
import Routes from 'routing/Routes';
import DocsHelper from 'util/DocsHelper';
import EventsPageNavigation from 'components/events/EventsPageNavigation';
import PageDescription from 'components/event-notifications/PageDescription';

const EventNotificationsPage = () => (
  <DocumentTitle title="Notifications">
    <EventsPageNavigation />
    <PageHeader
      title="Notifications"
      actions={
        <IfPermitted permissions="eventnotifications:create">
          <LinkContainer to={Routes.ALERTS.NOTIFICATIONS.CREATE}>
            <Button bsStyle="success">Create notification</Button>
          </LinkContainer>
        </IfPermitted>
      }
      documentationLink={{
        title: 'Alerts documentation',
        path: DocsHelper.PAGES.ALERTS,
      }}>
      <PageDescription />
    </PageHeader>

    <Row className="content">
      <Col md={12}>
        <EventNotificationsContainer />
      </Col>
    </Row>
  </DocumentTitle>
);

export default EventNotificationsPage;

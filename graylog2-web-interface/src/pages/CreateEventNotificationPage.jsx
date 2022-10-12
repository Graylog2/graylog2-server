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

import { Col, Row } from 'components/bootstrap';
import { DocumentTitle, PageHeader } from 'components/common';
import DocumentationLink from 'components/support/DocumentationLink';
import Routes from 'routing/Routes';
import DocsHelper from 'util/DocsHelper';
import connect from 'stores/connect';
import PermissionsMixin from 'util/PermissionsMixin';
import history from 'util/History';
import EventNotificationFormContainer from 'components/event-notifications/event-notification-form/EventNotificationFormContainer';
import { CurrentUserStore } from 'stores/users/CurrentUserStore';
import EventsSubareaNavigation from 'components/events/EventsSubareaNavigation';

const CreateEventDefinitionPage = ({ currentUser }) => {
  if (!PermissionsMixin.isPermitted(currentUser.permissions, 'eventnotifications:create')) {
    history.push(Routes.NOTFOUND);
  }

  return (
    <DocumentTitle title="New Notification">
      <EventsSubareaNavigation />
      <PageHeader title="New Notification">
        <span>
          Notifications alert you of any configured Event when they occur. Graylog can send Notifications directly
          to you or to other systems you use for that purpose.
        </span>

        <span>
          Graylog&apos;s new Alerting system let you define more flexible and powerful rules. Learn more in the{' '}
          <DocumentationLink page={DocsHelper.PAGES.ALERTS}
                             text="documentation" />
        </span>
      </PageHeader>

      <Row className="content">
        <Col md={12}>
          <EventNotificationFormContainer action="create" />
        </Col>
      </Row>
    </DocumentTitle>
  );
};

CreateEventDefinitionPage.propTypes = {
  currentUser: PropTypes.object.isRequired,
};

export default connect(CreateEventDefinitionPage, {
  currentUser: CurrentUserStore,
}, ({ currentUser }) => ({ currentUser: currentUser.currentUser }));

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
import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';

import { Col, Row } from 'components/bootstrap';
import { DocumentTitle, PageHeader } from 'components/common';
import EventDefinitionFormContainer
  from 'components/event-definitions/event-definition-form/EventDefinitionFormContainer';
import Routes from 'routing/Routes';
import DocsHelper from 'util/DocsHelper';
import { isPermitted } from 'util/PermissionsMixin';
import EventsPageNavigation from 'components/events/EventsPageNavigation';
import useCurrentUser from 'hooks/useCurrentUser';

const CreateEventDefinitionPage = () => {
  const currentUser = useCurrentUser();
  const navigate = useNavigate();
  const [eventDefinitionTitle, setEventDefinitionTitle] = useState();

  const handleEventDefinitionChange = useCallback((eventDefinition) => {
    if (eventDefinition.title !== eventDefinitionTitle) {
      setEventDefinitionTitle(eventDefinition.title);
    }
  }, [eventDefinitionTitle]);

  const pageTitle = useMemo(() => (eventDefinitionTitle ? `New Event Definition "${eventDefinitionTitle}"` : 'New Event Definition'), [eventDefinitionTitle]);

  useEffect(() => {
    if (!isPermitted(currentUser.permissions, 'eventdefinitions:create')) {
      navigate(Routes.NOTFOUND);
    }
  }, [currentUser.permissions, navigate]);

  return (
    <DocumentTitle title={pageTitle}>
      <EventsPageNavigation />

      <PageHeader title={pageTitle}
                  documentationLink={{
                    title: 'Alerts documentation',
                    path: DocsHelper.PAGES.ALERTS,
                  }}>
        <span>
          Event Definitions allow you to create Alerts from different Conditions and alert on them.
        </span>
      </PageHeader>

      <Row className="content">
        <Col md={12}>
          <EventDefinitionFormContainer action="create" onEventDefinitionChange={handleEventDefinitionChange} />
        </Col>
      </Row>

    </DocumentTitle>
  );
};

export default CreateEventDefinitionPage;

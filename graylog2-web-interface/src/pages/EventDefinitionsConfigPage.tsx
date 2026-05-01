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

import { Alert } from 'components/bootstrap';
import { DocumentTitle, PageHeader } from 'components/common';
import DocsHelper from 'util/DocsHelper';
import EventsPageNavigation from 'components/events/EventsPageNavigation';
import EventDefinitionsConfigList from 'components/event-definitions/config/EventDefinitionsConfigList';
import usePermissions from 'hooks/usePermissions';

const EventDefinitionsConfigPage = () => {
  const { isPermitted } = usePermissions();
  const canEdit = isPermitted('eventdefinitions:edit');

  return (
    <DocumentTitle title="Event Definitions Configuration">
      <EventsPageNavigation />
      <PageHeader
        title="Event Definitions Configuration"
        documentationLink={{
          title: 'Alerts documentation',
          path: DocsHelper.PAGES.ALERTS,
        }}>
        <span>Manage tags used to organize and categorize event definitions.</span>
      </PageHeader>

      {canEdit ? (
        <EventDefinitionsConfigList />
      ) : (
        <Alert bsStyle="warning" title="Insufficient permissions">
          You are missing sufficient permissions to manage event definition tags.
        </Alert>
      )}
    </DocumentTitle>
  );
};

export default EventDefinitionsConfigPage;

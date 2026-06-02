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

import { DocumentTitle, IfPermitted } from 'components/common';
import PageNavigation from 'components/common/PageNavigation';
import { SystemNotificationsTable } from 'components/notifications';
import SYSTEM_OVERVIEW_TABS from 'components/notifications/systemOverviewTabs';
import { Row } from 'components/bootstrap';

const SystemNotificationsPage = () => (
  <DocumentTitle title="System notifications">
    <span>
      <Row>
        <PageNavigation items={SYSTEM_OVERVIEW_TABS} />
      </Row>
      <IfPermitted permissions="notifications:read">
        <SystemNotificationsTable />
      </IfPermitted>
    </span>
  </DocumentTitle>
);

export default SystemNotificationsPage;

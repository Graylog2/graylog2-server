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
import { PAGE_NAV_TITLE } from 'components/system-overview/bindings';
import { Row } from 'components/bootstrap';
import { useTranslation } from 'react-i18next';

const SystemNotificationsPage = () => {
  const { t } = useTranslation('system');

  return (
    <DocumentTitle title={t('system_notifications_page.title')}>
      <span>
        <Row>
          <PageNavigation page={PAGE_NAV_TITLE} />
        </Row>
        <IfPermitted permissions="notifications:read">
          <SystemNotificationsTable />
        </IfPermitted>
      </span>
    </DocumentTitle>
  );
};

export default SystemNotificationsPage;

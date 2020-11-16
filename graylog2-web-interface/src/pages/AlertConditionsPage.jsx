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
import createReactClass from 'create-react-class';
import Reflux from 'reflux';

import { Col, Row } from 'components/graylog';
import DocumentationLink from 'components/support/DocumentationLink';
import { DocumentTitle, PageHeader } from 'components/common';
import { AlertsHeaderToolbar } from 'components/alerts';
import { AlertConditionsComponent } from 'components/alertconditions';
import Routes from 'routing/Routes';
import DocsHelper from 'util/DocsHelper';
import StoreProvider from 'injection/StoreProvider';

const CurrentUserStore = StoreProvider.getStore('CurrentUser');

const AlertConditionsPage = createReactClass({
  displayName: 'AlertConditionsPage',
  mixins: [Reflux.connect(CurrentUserStore)],

  render() {
    return (
      <DocumentTitle title="Alert conditions">
        <div>
          <PageHeader title="Manage alert conditions">
            <span>
              Alert conditions define situations that require your attention. Graylog will check those conditions
              periodically and notify you when their statuses change.
            </span>

            <span>
              Read more about alerting in the <DocumentationLink page={DocsHelper.PAGES.ALERTS} text="documentation" />.
            </span>

            <span>
              <AlertsHeaderToolbar active={Routes.LEGACY_ALERTS.CONDITIONS} />
            </span>
          </PageHeader>

          <Row className="content">
            <Col md={12}>
              <AlertConditionsComponent />
            </Col>
          </Row>
        </div>
      </DocumentTitle>
    );
  },
});

export default AlertConditionsPage;

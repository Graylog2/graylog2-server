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
// @flow strict
import * as React from 'react';

import AuthenticationAction from 'actions/authentication/AuthenticationActions';
import {} from 'components/authentication/bindings'; // Bind all authentication plugins
import { Alert, Row, Col } from 'components/graylog';
import { DocumentTitle, PageHeader, Icon } from 'components/common';
import DocsHelper from 'util/DocsHelper';
import DocumentationLink from 'components/support/DocumentationLink';
import BackendsOverview from 'components/authentication/BackendsOverview';
import AuthenticationOverviewLinks from 'components/authentication/AuthenticationOverviewLinks';
import BackendActionLinks from 'components/authentication/BackendActionLinks';
import useActiveBackend from 'components/authentication/useActiveBackend';

const AuthenticationOverviewPage = () => {
  const { finishedLoading, activeBackend, backendsTotal } = useActiveBackend([AuthenticationAction.setActiveBackend]);

  return (
    <DocumentTitle title="All Authentication Services">
      <>
        <PageHeader title="All Authentication Services"
                    subactions={(
                      <BackendActionLinks activeBackend={activeBackend}
                                          finishedLoading={finishedLoading} />
                  )}>
          <span>Configure Graylog&apos;s authentication services of this Graylog cluster.</span>
          <span>Read more authentication in the <DocumentationLink page={DocsHelper.PAGES.USERS_ROLES}
                                                                   text="documentation" />.
          </span>
          <AuthenticationOverviewLinks />
        </PageHeader>
        {!!(backendsTotal && backendsTotal >= 1 && !activeBackend) && (
        <Row className="content">
          <Col xs={12}>
            <Alert bsStyle="warning">
              <Icon name="exclamation-circle" /> None of the configured authentication services is currently active.
            </Alert>
          </Col>
        </Row>
        )}
        <BackendsOverview />
      </>
    </DocumentTitle>
  );
};

export default AuthenticationOverviewPage;

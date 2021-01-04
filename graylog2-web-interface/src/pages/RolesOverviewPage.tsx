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

import { LinkContainer } from 'components/graylog/router';
import RolesOverview from 'components/roles/RolesOverview';
import Routes from 'routing/Routes';
import DocsHelper from 'util/DocsHelper';
import { Button, Row, Col, Alert } from 'components/graylog';
import { PageHeader, DocumentTitle, Icon } from 'components/common';
import DocumentationLink from 'components/support/DocumentationLink';

const RolesOverviewPage = () => (
  <DocumentTitle title="Roles Overview">
    <PageHeader title="Roles Overview">
      <span>Overview of Graylog&apos;s roles. Roles allow granting capabilities to users, like creating dashboards or event definitions.</span>

      <span>
        Learn more in the{' '}
        <DocumentationLink page={DocsHelper.PAGES.USERS_ROLES}
                           text="documentation" />
      </span>

      <LinkContainer to={Routes.SYSTEM.AUTHZROLES.OVERVIEW}>
        <Button bsStyle="info">Roles Overview</Button>
      </LinkContainer>
    </PageHeader>

    <Row className="content">
      <Col xs={12}>
        <Alert bsStyle="info">
          <Icon name="info-circle" />{' '}<b>Granting Permissions</b><br />
          With Graylog 4.0, we&apos;ve updated the permissions system and changed the purpose of roles.
          The built-in roles still allow granting capabilities to users, like creating dashboards or viewing the archive catalog.
          But they no longer grant permissions for a specific dashboard or stream. It is also not possible to create an own role.
          Granting permissions for a specific entity can now be done by using its <b><Icon name="user-plus" /> Share</b> button. You can find the button e.g. on the entities overview page.
          If you want to grant permissions for an entity to multiple users at once, you can use teams.
          Learn more in the <DocumentationLink page={DocsHelper.PAGES.PERMISSIONS} text="documentation" />.
        </Alert>
      </Col>
    </Row>

    <RolesOverview />
  </DocumentTitle>
);

export default RolesOverviewPage;

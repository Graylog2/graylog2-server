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

import { LinkContainer } from 'components/common/router';
import Routes from 'routing/Routes';
import DocsHelper from 'util/DocsHelper';
import { Button } from 'components/bootstrap';
import { PageHeader, DocumentTitle } from 'components/common';
import UsersOverview from 'components/users/UsersOverview';
import UsersPageNavigation from 'components/users/navigation/UsersPageNavigation';
import DocumentationLink from 'components/support/DocumentationLink';

const UsersOverviewPage = () => (
  <DocumentTitle title="Users Overview">
    <UsersPageNavigation />
    <PageHeader title="Users Overview"
                subactions={(
                  <LinkContainer to={Routes.SYSTEM.USERS.CREATE}>
                    <Button bsStyle="success">Create user</Button>
                  </LinkContainer>
                )}>
      <span>Overview of Graylog&apos;s registered users.</span>

      <span>
        Learn more in the{' '}
        <DocumentationLink page={DocsHelper.PAGES.USERS_ROLES}
                           text="documentation" />
      </span>
    </PageHeader>

    <UsersOverview />
  </DocumentTitle>
);

export default UsersOverviewPage;

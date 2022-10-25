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
import UserCreate from 'components/users/UserCreate';
import UsersPageNavigation from 'components/users/navigation/UsersPageNavigation';

const UserCreatePage = () => (
  <DocumentTitle title="Create New User">
    <UsersPageNavigation />
    <PageHeader title="Create New User"
                actions={(
                  <LinkContainer to={Routes.SYSTEM.USERS.CREATE}>
                    <Button bsStyle="success">Create user</Button>
                  </LinkContainer>
                )}
                documentationLink={{
                  title: 'Permissions documentation',
                  path: DocsHelper.PAGES.USERS_ROLES,
                }}>
      <span>
        Use this page to create new Graylog users. The users and their permissions created here are not limited
        to the web interface but valid and required for the REST APIs of your Graylog server nodes, too.
      </span>
    </PageHeader>

    <UserCreate />
  </DocumentTitle>
);

export default UserCreatePage;

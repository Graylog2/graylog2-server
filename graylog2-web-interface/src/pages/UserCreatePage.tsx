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

import Routes from 'routing/Routes';
import DocsHelper from 'util/DocsHelper';
import { CreatePage } from 'components/common';
import UsersPageNavigation from 'components/users/navigation/UsersPageNavigation';
import UserCreate from 'components/users/UserCreate';
import useUserCreateValidate from 'components/users/UserCreate/useUserCreateValidate';
import useUserCreateSubmit from 'components/users/UserCreate/useUserCreateSubmit';

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type FormValues = Record<string, any>;

const UserCreatePage = () => {
  const validate = useUserCreateValidate();
  const createUser = useUserCreateSubmit();

  const handleSubmit = async (values: FormValues): Promise<{ id: string }> => {
    const createdUser = await createUser(values);

    return { id: createdUser.id };
  };

  return (
    <>
      <UsersPageNavigation />
      <CreatePage<FormValues>
        entityName="User"
        overviewRoute={Routes.SYSTEM.USERS.OVERVIEW}
        detailsRoute={Routes.SYSTEM.USERS.show}
        initialValues={{ roles: ['Reader'] }}
        onSubmit={handleSubmit}
        validate={validate}
        documentationLink={{
          title: 'Permissions documentation',
          path: DocsHelper.PAGES.USERS_ROLES,
        }}
        description="Use this page to create new users for the web interface or the REST API.">
        <UserCreate />
      </CreatePage>
    </>
  );
};

export default UserCreatePage;

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
import { useEffect, useState } from 'react';

import { UsersActions } from 'stores/users/UsersStore';
import withParams from 'routing/withParams';
import DocsHelper from 'util/DocsHelper';
import UsersDomain from 'domainActions/users/UsersDomain';
import { PageHeader, DocumentTitle } from 'components/common';
import UserEdit from 'components/users/UserEdit';
import UsersPageNavigation from 'components/users/navigation/UsersPageNavigation';
import UserActionLinks from 'components/users/navigation/UserActionLinks';
import type User from 'logic/users/User';

type Props = {
  params: {
    userId: string,
  },
};

const PageTitle = ({ fullName }: { fullName: string | null | undefined }) => (
  <>
    Edit User {fullName && (
      <>
        - <i>{fullName}</i>
      </>
  )}
  </>
);

const _updateUserOnLoad = (setLoadedUser) => UsersActions.load.completed.listen(setLoadedUser);

const UserEditPage = ({ params }: Props) => {
  const [loadedUser, setLoadedUser] = useState<User | undefined>();
  const userId = params?.userId;

  // We need to trigger a user state update in child components and do so by calling the load action
  // and by defining a listener for this action which updates the state.
  useEffect(() => _updateUserOnLoad(setLoadedUser), []);

  useEffect(() => {
    UsersDomain.load(userId);
  }, [userId]);

  const fullName = loadedUser?.fullName ?? '';
  const readOnly = loadedUser?.readOnly ?? false;
  const userToEdit = userId === loadedUser?.id ? loadedUser : undefined;

  return (
    <DocumentTitle title={`Edit User ${fullName}`}>
      <UsersPageNavigation />
      <PageHeader title={<PageTitle fullName={fullName} />}
                  actions={(
                    <UserActionLinks userId={userId}
                                     userIsReadOnly={readOnly} />
                  )}
                  documentationLink={{
                    title: 'Permissions documentation',
                    path: DocsHelper.PAGES.USERS_ROLES,
                  }}>
        <span>
          You can change the user details and password here and assign roles and teams.
        </span>
      </PageHeader>
      <UserEdit user={userToEdit} />
    </DocumentTitle>
  );
};

export default withParams(UserEditPage);

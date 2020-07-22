// @flow strict
import * as React from 'react';
import { useEffect } from 'react';
import { withRouter } from 'react-router';

import DocsHelper from 'util/DocsHelper';
import { useStore } from 'stores/connect';
import { UsersActions, UsersStore } from 'stores/users/UsersStore';
import { PageHeader, DocumentTitle } from 'components/common';
import UserEdit from 'components/users/UserEdit';
import DocumentationLink from 'components/support/DocumentationLink';
import UserManagementLinks from 'components/users/UserManagementLinks';

type Props = {
  params: {
    username: string,
  },
};

const PageTitle = ({ fullName }: {fullName: ?string}) => (
  <>
    Edit User {fullName && (
      <>
        - <i>{fullName}</i>
      </>
  )}
  </>
);

const UserEditPage = ({ params }: Props) => {
  const { loadedUser } = useStore(UsersStore);
  const username = params?.username;

  useEffect(() => {
    UsersActions.load(username);
  });

  return (
    <DocumentTitle title={`Edit User ${loadedUser?.fullName ?? ''}`}>
      <PageHeader title={<PageTitle fullName={loadedUser?.fullName} />}>
        <span>
          You can change the user details and password here and assign roles and teams.
        </span>

        <span>
          Learn more in the{' '}
          <DocumentationLink page={DocsHelper.PAGES.USERS_ROLES}
                             text="documentation" />
        </span>

        <UserManagementLinks username={username}
                             userIsReadOnly={loadedUser?.readOnly} />
      </PageHeader>

      <UserEdit user={username === params?.username ? loadedUser : undefined} />
    </DocumentTitle>
  );
};

export default withRouter(UserEditPage);

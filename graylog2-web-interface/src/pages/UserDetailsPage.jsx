// @flow strict
import * as React from 'react';
import { useEffect, useState } from 'react';
import { withRouter } from 'react-router';

import { EntityShareActions } from 'stores/permissions/EntityShareStore';
import DocsHelper from 'util/DocsHelper';
import { UsersActions } from 'stores/users/UsersStore';
import { PageHeader, DocumentTitle } from 'components/common';
import UserDetails from 'components/users/UserDetails';
import UserManagementLinks from 'components/users/UserManagementLinks';
import DocumentationLink from 'components/support/DocumentationLink';

type Props = {
  params: {
    username: string,
  },
};

const PageTitle = ({ fullName }: {fullName: ?string}) => (
  <>
    User Details {fullName && (
      <>
        - <i>{fullName}</i>
      </>
  )}
  </>
);

const UserDetailsPage = ({ params }: Props) => {
  const [paginatedUserShares, setPaginatedUserShares] = useState();
  const [loadedUser, setLoadedUser] = useState();
  const username = params?.username;

  useEffect(() => {
    UsersActions.load(username).then(setLoadedUser);

    EntityShareActions.searchPaginatedUserShares(username, 1, 10, '').then((response) => {
      setPaginatedUserShares(response);
    });
  }, []);

  return (
    <DocumentTitle title={`User Details ${username ?? ''}`}>
      <PageHeader title={<PageTitle fullName={loadedUser?.fullName} />}>
        <span>
          Overview of details like profile information, settings, teams and roles.
        </span>

        <span>
          Learn more in the{' '}
          <DocumentationLink page={DocsHelper.PAGES.USERS_ROLES}
                             text="documentation" />
        </span>

        <UserManagementLinks username={username}
                             userIsReadOnly={loadedUser?.readOnly ?? false} />
      </PageHeader>

      <UserDetails paginatedUserShares={paginatedUserShares}
                   user={username === loadedUser?.username ? loadedUser : undefined} />
    </DocumentTitle>
  );
};

export default withRouter(UserDetailsPage);

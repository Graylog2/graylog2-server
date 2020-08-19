// @flow strict
import * as React from 'react';
import { useEffect, useState } from 'react';
import { withRouter } from 'react-router';

import DocsHelper from 'util/DocsHelper';
import UsersDomain from 'domainActions/users/UsersDomain';
import { UsersActions } from 'stores/users/UsersStore';
import { PageHeader, DocumentTitle } from 'components/common';
import UserEdit from 'components/users/UserEdit';
import DocumentationLink from 'components/support/DocumentationLink';
import UserOverviewLinks from 'components/users/navigation/UserOverviewLinks';
import UserActionLinks from 'components/users/navigation/UserActionLinks';

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
  const [loadedUser, setLoadedUser] = useState();
  const username = params?.username;

  useEffect(() => {
    UsersDomain.load(username);

    const unlistenLoadUser = UsersActions.load.completed.listen(setLoadedUser);

    return () => { unlistenLoadUser(); };
  }, [username]);

  return (
    <DocumentTitle title={`Edit User ${loadedUser?.fullName ?? ''}`}>
      <PageHeader title={<PageTitle fullName={loadedUser?.fullName} />}
                  subactions={(
                    <UserActionLinks username={username}
                                     userIsReadOnly={loadedUser?.readOnly ?? false} />
                  )}>
        <span>
          You can change the user details and password here and assign roles and teams.
        </span>

        <span>
          Learn more in the{' '}
          <DocumentationLink page={DocsHelper.PAGES.USERS_ROLES}
                             text="documentation" />
        </span>

        <UserOverviewLinks />
      </PageHeader>
      <UserEdit user={username === loadedUser?.username ? loadedUser : undefined} />
    </DocumentTitle>
  );
};

export default withRouter(UserEditPage);

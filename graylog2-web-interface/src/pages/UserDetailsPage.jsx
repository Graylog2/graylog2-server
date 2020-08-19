// @flow strict
import * as React from 'react';
import { useEffect, useState } from 'react';
import { withRouter } from 'react-router';

import { EntityShareActions } from 'stores/permissions/EntityShareStore';
import DocsHelper from 'util/DocsHelper';
import UsersDomain from 'domainActions/users/UsersDomain';
import { PageHeader, DocumentTitle } from 'components/common';
import UserDetails from 'components/users/UserDetails';
import UserOverviewLinks from 'components/users/navigation/UserOverviewLinks';
import UserActionLinks from 'components/users/navigation/UserActionLinks';
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
    UsersDomain.load(username).then(setLoadedUser);

    EntityShareActions.loadUserSharesPaginated(username, 1, 10, '').then((response) => {
      setPaginatedUserShares(response);
    });
  }, [username]);

  return (
    <DocumentTitle title={`User Details ${username ?? ''}`}>
      <PageHeader title={<PageTitle fullName={loadedUser?.fullName} />}
                  subactions={(
                    <UserActionLinks username={username}
                                     userIsReadOnly={loadedUser?.readOnly ?? false} />
                  )}>
        <span>
          Overview of details like profile information, settings, teams and roles.
        </span>

        <span>
          Learn more in the{' '}
          <DocumentationLink page={DocsHelper.PAGES.USERS_ROLES}
                             text="documentation" />
        </span>

        <UserOverviewLinks />
      </PageHeader>

      <UserDetails paginatedUserShares={paginatedUserShares}
                   user={username === loadedUser?.username ? loadedUser : undefined} />
    </DocumentTitle>
  );
};

export default withRouter(UserDetailsPage);

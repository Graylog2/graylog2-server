// @flow strict
import * as React from 'react';
import { useEffect, useState } from 'react';
import { withRouter } from 'react-router';

import { PageHeader, DocumentTitle } from 'components/common';
import EntityShareDomain from 'domainActions/permissions/EntityShareDomain';
import DocsHelper from 'util/DocsHelper';
import UsersDomain from 'domainActions/users/UsersDomain';
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

    EntityShareDomain.loadUserSharesPaginated(username, 1, 10, '')
      .then((newPaginatedUserShares) => newPaginatedUserShares && setPaginatedUserShares(newPaginatedUserShares));
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

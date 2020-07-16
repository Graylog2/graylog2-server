// @flow strict
import * as React from 'react';
import { useEffect } from 'react';
import { LinkContainer } from 'react-router-bootstrap';
import { withRouter } from 'react-router';

import Routes from 'routing/Routes';
import DocsHelper from 'util/DocsHelper';
import { useStore } from 'stores/connect';
import { UsersActions, UsersStore } from 'stores/users/UsersStore';
import { ButtonToolbar, Button } from 'components/graylog';
import { PageHeader, DocumentTitle } from 'components/common';
import UserDetails from 'components/users/UserDetails';
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
  const { loadedUser } = useStore(UsersStore);

  useEffect(() => {
    UsersActions.load(params?.username);
  });

  return (
    <DocumentTitle title={`User Detials ${loadedUser?.username ?? ''}`}>
      <PageHeader title={<PageTitle fullName={loadedUser?.fullName} />}>
        <span />

        <span>
          Learn more in the{' '}
          <DocumentationLink page={DocsHelper.PAGES.USERS_ROLES}
                             text="documentation" />
        </span>

        <ButtonToolbar>
          {loadedUser && !loadedUser.readOnly && (
            <LinkContainer to={Routes.SYSTEM.USERS.edit(loadedUser?.username)}>
              <Button bsStyle="success">Edit User</Button>
            </LinkContainer>
          )}
          <LinkContainer to={Routes.SYSTEM.USERS.OVERVIEW}>
            <Button bsStyle="info">Users Overview</Button>
          </LinkContainer>
        </ButtonToolbar>
      </PageHeader>

      <UserDetails user={loadedUser?.username === params?.username ? loadedUser : undefined} />
    </DocumentTitle>
  );
};

export default withRouter(UserDetailsPage);

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
import UserEdit from 'components/users/UserEdit';
import DocumentationLink from 'components/support/DocumentationLink';

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

  useEffect(() => {
    UsersActions.load(params?.username);
  });

  return (
    <DocumentTitle title={`Edit User ${loadedUser?.fullName ?? ''}`}>
      <PageHeader title={<PageTitle fullName={loadedUser?.fullName} />}>
        <span />

        <span>
          Learn more in the{' '}
          <DocumentationLink page={DocsHelper.PAGES.USERS_ROLES}
                             text="documentation" />
        </span>

        <ButtonToolbar>
          <LinkContainer to={Routes.SYSTEM.USERS.show(loadedUser?.username)}>
            <Button bsStyle="success">User Details</Button>
          </LinkContainer>
          <LinkContainer to={Routes.SYSTEM.USERS.OVERVIEW}>
            <Button bsStyle="info">Users Overview</Button>
          </LinkContainer>
        </ButtonToolbar>
      </PageHeader>

      <UserEdit user={loadedUser?.username === params?.username ? loadedUser : undefined} />
    </DocumentTitle>
  );
};

export default withRouter(UserEditPage);

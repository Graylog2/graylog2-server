// @flow strict
import * as React from 'react';
import { useEffect } from 'react';
import { LinkContainer } from 'react-router-bootstrap';
import { withRouter } from 'react-router';

import UserEdit from 'components/users/UserEdit';
import { useStore } from 'stores/connect';
import UsersActions from 'actions/users/UsersActions';
import UsersStore from 'stores/users/UsersStore';
import Routes from 'routing/Routes';
import DocsHelper from 'util/DocsHelper';
import { ButtonToolbar, Button } from 'components/graylog';
import { PageHeader } from 'components/common';
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
    <div>
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
            <Button bsStyle="info">Users</Button>
          </LinkContainer>
        </ButtonToolbar>
      </PageHeader>

      <UserEdit user={loadedUser?.username === params?.username ? loadedUser : undefined} />
    </div>
  );
};

export default withRouter(UserEditPage);

// @flow strict
import * as React from 'react';
import { LinkContainer } from 'react-router-bootstrap';
import UserCreate from 'components/users/UserCreate';

import Routes from 'routing/Routes';
import DocsHelper from 'util/DocsHelper';
import { ButtonToolbar, Button } from 'components/graylog';
import { PageHeader } from 'components/common';
import DocumentationLink from 'components/support/DocumentationLink';

const UserCreatePage = () => {
  return (
    <div>
      <PageHeader title="Create user">
        <span />

        <span>
          Learn more in the{' '}
          <DocumentationLink page={DocsHelper.PAGES.USERS_ROLES}
                             text="documentation" />
        </span>

        <ButtonToolbar>
          <LinkContainer to={Routes.SYSTEM.USERS.OVERVIEW}>
            <Button bsStyle="info">Users</Button>
          </LinkContainer>
        </ButtonToolbar>
      </PageHeader>

      <UserCreate />
    </div>
  );
};

export default UserCreatePage;

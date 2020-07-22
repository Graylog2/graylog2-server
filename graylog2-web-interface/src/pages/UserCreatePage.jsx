// @flow strict
import * as React from 'react';
import { LinkContainer } from 'react-router-bootstrap';

import UserCreate from 'components/users/UserCreate';
import Routes from 'routing/Routes';
import DocsHelper from 'util/DocsHelper';
import { ButtonToolbar, Button } from 'components/graylog';
import { PageHeader, DocumentTitle } from 'components/common';
import DocumentationLink from 'components/support/DocumentationLink';

const UserCreatePage = () => (
  <DocumentTitle title="Create New User">
    <PageHeader title="Create New User">
      <span>
        Use this page to create new Graylog users. The users and their permissions created here are not limited
        to the web interface but valid and required for the REST APIs of your Graylog server nodes, too.
      </span>

      <span>
        Learn more in the{' '}
        <DocumentationLink page={DocsHelper.PAGES.USERS_ROLES}
                           text="documentation" />
      </span>

      <ButtonToolbar>
        <LinkContainer to={Routes.SYSTEM.USERS.OVERVIEW}>
          <Button bsStyle="info">Users Overview</Button>
        </LinkContainer>
      </ButtonToolbar>
    </PageHeader>

    <UserCreate />
  </DocumentTitle>
);

export default UserCreatePage;

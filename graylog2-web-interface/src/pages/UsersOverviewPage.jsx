// @flow strict
import * as React from 'react';
import { LinkContainer } from 'react-router-bootstrap';

import Routes from 'routing/Routes';
import DocsHelper from 'util/DocsHelper';
import { ButtonToolbar, Button } from 'components/graylog';
import { PageHeader } from 'components/common';
import UsersOverview from 'components/users/UsersOverview';
import DocumentationLink from 'components/support/DocumentationLink';

const UsersOverviewPage = () => {
  return (
    <div>
      <PageHeader title="Users Overview">
        <span>Overview of Graylog&apos;s registered users.</span>

        <span>
          Learn more in the{' '}
          <DocumentationLink page={DocsHelper.PAGES.USERS_ROLES}
                             text="documentation" />
        </span>

        <ButtonToolbar>
          <LinkContainer to={Routes.SYSTEM.USERS.OVERVIEW}>
            <Button bsStyle="info" className="active">Users</Button>
          </LinkContainer>

          <LinkContainer to={Routes.SYSTEM.USERS.OVERVIEW}>
            <Button bsStyle="info">Teams</Button>
          </LinkContainer>

        </ButtonToolbar>
      </PageHeader>

      <UsersOverview />
    </div>
  );
};

export default UsersOverviewPage;

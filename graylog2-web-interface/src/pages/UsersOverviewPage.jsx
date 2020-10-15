// @flow strict
import * as React from 'react';
import { LinkContainer } from 'react-router-bootstrap';

import Routes from 'routing/Routes';
import DocsHelper from 'util/DocsHelper';
import { Button } from 'components/graylog';
import { PageHeader, DocumentTitle } from 'components/common';
import UsersOverview from 'components/users/UsersOverview';
import UserOverviewLinks from 'components/users/navigation/UserOverviewLinks';
import DocumentationLink from 'components/support/DocumentationLink';

const UsersOverviewPage = () => (
  <DocumentTitle title="Users Overview">
    <PageHeader title="Users Overview"
                subactions={(
                  <LinkContainer to={Routes.SYSTEM.USERS.CREATE}>
                    <Button bsStyle="success">Create User</Button>
                  </LinkContainer>
                )}>
      <span>Overview of Graylog&apos;s registered users.</span>

      <span>
        Learn more in the{' '}
        <DocumentationLink page={DocsHelper.PAGES.USERS_ROLES}
                           text="documentation" />
      </span>

      <UserOverviewLinks />
    </PageHeader>

    <UsersOverview />
  </DocumentTitle>
);

export default UsersOverviewPage;

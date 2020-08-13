// @flow strict
import * as React from 'react';
import { LinkContainer } from 'react-router-bootstrap';

import RolesOverview from 'components/roles/RolesOverview';
import Routes from 'routing/Routes';
import DocsHelper from 'util/DocsHelper';
import { ButtonToolbar, Button } from 'components/graylog';
import { PageHeader, DocumentTitle } from 'components/common';
import DocumentationLink from 'components/support/DocumentationLink';

const RolesOverviewPage = () => (
  <DocumentTitle title="Roles Overview">
    <PageHeader title="Roles Overview">
      <span>Overview of Graylog&apos;s roles.</span>

      <span>
        Learn more in the{' '}
        <DocumentationLink page={DocsHelper.PAGES.USERS_ROLES}
                           text="documentation" />
      </span>

      <ButtonToolbar>
        <LinkContainer to={Routes.SYSTEM.AUTHZROLES.OVERVIEW}>
          <Button bsStyle="info">Roles Overview</Button>
        </LinkContainer>
      </ButtonToolbar>
    </PageHeader>

    <RolesOverview />
  </DocumentTitle>
);

export default RolesOverviewPage;

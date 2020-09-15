// @flow strict
import * as React from 'react';
import { useState, useEffect } from 'react';
import { LinkContainer } from 'react-router-bootstrap';

import BackendOverviewLinks from 'components/authentication/BackendOverviewLinks';
import BackendDetails from 'components/authentication/BackendDetails';
import DocsHelper from 'util/DocsHelper';
import BackendsOverview from 'components/authentication/BackendsOverview';
import DocumentationLink from 'components/support/DocumentationLink';
import BackendCreateGettingStarted from 'components/authentication/BackendCreateGettingStarted';
import { PageHeader, Spinner, DocumentTitle } from 'components/common';
import { ButtonToolbar, Button } from 'components/graylog';
import Routes from 'routing/Routes';
import AuthenticationDomain from 'domainActions/authentication/AuthenticationDomain';

const DEFAULT_PAGINATION = {
  count: undefined,
  total: undefined,
  page: 1,
  perPage: 10,
  query: '',
};

const AuthenticationPage = () => {
  const [paginatedAuthBackends, setPaginatedAuthBackends] = useState();

  useEffect(() => {
    AuthenticationDomain.loadBackendsPaginated(DEFAULT_PAGINATION.page, DEFAULT_PAGINATION.perPage, DEFAULT_PAGINATION.query).then((newServices) => newServices && setPaginatedAuthBackends(newServices));
  }, []);

  if (!paginatedAuthBackends) {
    return <Spinner />;
  }

  const activeBackend = paginatedAuthBackends.list.find((backend) => backend.id === paginatedAuthBackends.globalConfig.activeBackend);

  return (
    <DocumentTitle title="Authentication Management">
      <>
        <PageHeader title="Authentication Management"
                    subactions={(activeBackend && (
                    <ButtonToolbar>
                      <LinkContainer to={Routes.SYSTEM.AUTHENTICATION.PROVIDERS.edit(activeBackend.id)}>
                        <Button bsStyle="success">
                          Edit Active Backend
                        </Button>
                      </LinkContainer>
                      <LinkContainer to={Routes.SYSTEM.AUTHENTICATION.PROVIDERS.CREATE}>
                        <Button bsStyle="success">
                          Create Backend
                        </Button>
                      </LinkContainer>
                    </ButtonToolbar>
                    ))}>
          <span>Configure Graylog&apos;s authentication providers of this Graylog cluster.</span>
          <span>Read more authentication in the <DocumentationLink page={DocsHelper.PAGES.USERS_ROLES}
                                                                   text="documentation" />.
          </span>
          <BackendOverviewLinks />
        </PageHeader>

        {!activeBackend && <BackendCreateGettingStarted />}

        {activeBackend && <BackendDetails authenticationBackend={activeBackend} />}

        {paginatedAuthBackends.list.size >= 1 && (
        <BackendsOverview paginatedAuthBackends={paginatedAuthBackends} />
        )}
      </>
    </DocumentTitle>
  );
};

export default AuthenticationPage;

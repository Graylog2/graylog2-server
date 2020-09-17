// @flow strict
import * as React from 'react';
import { useState, useEffect } from 'react';
import { LinkContainer } from 'react-router-bootstrap';

import {} from 'components/authentication'; // Bind all authentication plugins
import BackendOverviewLinks from 'components/authentication/BackendOverviewLinks';
import BackendDetails from 'components/authentication/BackendDetails';
import DocsHelper from 'util/DocsHelper';
import DocumentationLink from 'components/support/DocumentationLink';
import BackendCreateGettingStarted from 'components/authentication/BackendCreateGettingStarted';
import { PageHeader, Spinner, DocumentTitle } from 'components/common';
import { ButtonToolbar, Button } from 'components/graylog';
import Routes from 'routing/Routes';
import AuthenticationDomain from 'domainActions/authentication/AuthenticationDomain';
import AuthenticationBackend from 'logic/authentication/AuthenticationBackend';
import StringUtils from 'util/StringUtils';

const _pageTilte = (activeBackend: ?AuthenticationBackend) => {
  const title = 'Authentication Services';

  if (activeBackend) {
    const backendTitle = StringUtils.truncateWithEllipses(activeBackend.title, 30);

    return <>{title} - <i>{backendTitle}</i></>;
  }

  return title;
};

const AuthenticationPage = () => {
  const [activeBackend, setActiveBackend] = useState();
  const [finishedLoading, setFinishedLoading] = useState(false);
  const pageTitle = _pageTilte(activeBackend);

  useEffect(() => {
    AuthenticationDomain.loadActive().then((backend) => {
      setFinishedLoading(true);

      if (backend) {
        setActiveBackend(backend);
      }
    });
  }, []);

  if (!finishedLoading) {
    return <Spinner />;
  }

  return (
    <DocumentTitle title={pageTitle}>
      <>
        <PageHeader title={pageTitle}
                    subactions={(activeBackend && (
                      <ButtonToolbar>
                        <LinkContainer to={Routes.SYSTEM.AUTHENTICATION.PROVIDERS.edit(activeBackend.id)}>
                          <Button bsStyle="success">
                            Edit Active Service
                          </Button>
                        </LinkContainer>
                        <LinkContainer to={Routes.SYSTEM.AUTHENTICATION.PROVIDERS.CREATE}>
                          <Button bsStyle="success">
                            Create Service
                          </Button>
                        </LinkContainer>
                      </ButtonToolbar>
                    ))}>
          <span>Configure Graylog&apos;s authentication services of this Graylog cluster.</span>
          <span>Read more authentication in the <DocumentationLink page={DocsHelper.PAGES.USERS_ROLES}
                                                                   text="documentation" />.
          </span>
          <BackendOverviewLinks />
        </PageHeader>

        {!activeBackend && <BackendCreateGettingStarted />}

        {activeBackend && <BackendDetails authenticationBackend={activeBackend} />}
      </>
    </DocumentTitle>
  );
};

export default AuthenticationPage;

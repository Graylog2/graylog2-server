// @flow strict
import * as React from 'react';

import {} from 'components/authentication/bindings'; // Bind all authentication plugins
import DocsHelper from 'util/DocsHelper';
import StringUtils from 'util/StringUtils';
import AuthenticationBackend from 'logic/authentication/AuthenticationBackend';
import history from 'util/History';
import Routes from 'routing/Routes';
import { useActiveBackend } from 'components/authentication/hooks';
import { PageHeader, Spinner, DocumentTitle } from 'components/common';
import BackendActionLinks from 'components/authentication/BackendActionLinks';
import GettingStarted from 'components/authentication/BackendCreate/GettingStarted';
import BackendDetailsActive from 'components/authentication/BackendDetailsActive';
import BackendOverviewLinks from 'components/authentication/BackendOverviewLinks';
import DocumentationLink from 'components/support/DocumentationLink';

const _pageTilte = (activeBackend: ?AuthenticationBackend) => {
  const title = 'Active Authentication Service';

  if (activeBackend) {
    const backendTitle = StringUtils.truncateWithEllipses(activeBackend.title, 30);

    return <>{title} - <i>{backendTitle}</i></>;
  }

  return title;
};

const AuthenticationPage = () => {
  const { finishedLoading, activeBackend, backendsTotal } = useActiveBackend();
  const pageTitle = _pageTilte(activeBackend);

  if (!finishedLoading) {
    return <Spinner />;
  }

  // Only display this page if there is an active backend
  // Otherwise redirect to correct page
  if (finishedLoading && !activeBackend && backendsTotal === 0) {
    history.push(Routes.SYSTEM.AUTHENTICATION.BACKENDS.CREATE);
  } else if (finishedLoading && !activeBackend && backendsTotal) {
    history.push(Routes.SYSTEM.AUTHENTICATION.BACKENDS.OVERVIEW);
  }

  return (
    <DocumentTitle title={pageTitle}>
      <>
        <PageHeader title={pageTitle}
                    subactions={(
                      <BackendActionLinks activeBackend={activeBackend}
                                          finishedLoading={finishedLoading} />
                    )}>
          <span>Configure Graylog&apos;s authentication services of this Graylog cluster.</span>
          <span>Read more authentication in the <DocumentationLink page={DocsHelper.PAGES.USERS_ROLES}
                                                                   text="documentation" />.
          </span>
          <BackendOverviewLinks activeBackend={activeBackend}
                                finishedLoading={finishedLoading} />
        </PageHeader>

        {finishedLoading && activeBackend && (
          <BackendDetailsActive authenticationBackend={activeBackend} />
        )}
      </>
    </DocumentTitle>
  );
};

export default AuthenticationPage;

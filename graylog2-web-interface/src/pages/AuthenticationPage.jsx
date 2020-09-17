// @flow strict
import * as React from 'react';

import {} from 'components/authentication'; // Bind all authentication plugins
import BackendOverviewLinks from 'components/authentication/BackendOverviewLinks';
import BackendDetails from 'components/authentication/BackendDetails';
import DocsHelper from 'util/DocsHelper';
import { useActiveBackend } from 'components/authentication/hooks';
import DocumentationLink from 'components/support/DocumentationLink';
import BackendCreateGettingStarted from 'components/authentication/BackendCreateGettingStarted';
import { PageHeader, Spinner, DocumentTitle } from 'components/common';
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
  const { finishedLoading, activeBackend } = useActiveBackend();
  const pageTitle = _pageTilte(activeBackend);

  if (!finishedLoading) {
    return <Spinner />;
  }

  return (
    <DocumentTitle title={pageTitle}>
      <>
        <PageHeader title={pageTitle}>
          <span>Configure Graylog&apos;s authentication services of this Graylog cluster.</span>
          <span>Read more authentication in the <DocumentationLink page={DocsHelper.PAGES.USERS_ROLES}
                                                                   text="documentation" />.
          </span>
          <BackendOverviewLinks activeBackend={activeBackend}
                                finishedLoading={finishedLoading} />
        </PageHeader>

        {!finishedLoading && (
          <Spinner />
        )}

        {finishedLoading && (
          <>
            {!activeBackend && <BackendCreateGettingStarted />}
            {activeBackend && <BackendDetails authenticationBackend={activeBackend} />}
          </>
        )}
      </>
    </DocumentTitle>
  );
};

export default AuthenticationPage;

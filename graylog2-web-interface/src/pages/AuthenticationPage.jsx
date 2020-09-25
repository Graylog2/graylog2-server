// @flow strict
import * as React from 'react';

import {} from 'components/authentication/bindings'; // Bind all authentication plugins
import DocsHelper from 'util/DocsHelper';
import StringUtils from 'util/StringUtils';
import AuthenticationBackend from 'logic/authentication/AuthenticationBackend';
import { useActiveBackend } from 'components/authentication/hooks';
import { PageHeader, Spinner, DocumentTitle } from 'components/common';
import BackendActionLinks from 'components/authentication/BackendActionLinks';
import GettingStarted from 'components/authentication/BackendCreate/GettingStarted';
import BackendDetailsActive from 'components/authentication/BackendDetailsActive';
import BackendOverviewLinks from 'components/authentication/BackendOverviewLinks';
import DocumentationLink from 'components/support/DocumentationLink';

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
          <BackendOverviewLinks />
        </PageHeader>

        {!finishedLoading && (
          <Spinner />
        )}

        {finishedLoading && (
          <>
            {!activeBackend && <GettingStarted />}
            {activeBackend && <BackendDetailsActive authenticationBackend={activeBackend} />}
          </>
        )}
      </>
    </DocumentTitle>
  );
};

export default AuthenticationPage;

// @flow strict
import * as React from 'react';
import { useEffect } from 'react';
import {} from 'components/authentication/bindings'; // Bind all authentication plugins

import DocsHelper from 'util/DocsHelper';
import withParams from 'routing/withParams';
import StringUtils from 'util/StringUtils';
import AuthenticationBackend from 'logic/authentication/AuthenticationBackend';
import history from 'util/History';
import Routes from 'routing/Routes';
import useActiveBackend from 'components/authentication/useActiveBackend';
import { PageHeader, Spinner, DocumentTitle } from 'components/common';
import BackendActionLinks from 'components/authentication/BackendActionLinks';
import BackendDetailsActive from 'components/authentication/BackendDetailsActive';
import BackendOverviewLinks from 'components/authentication/BackendOverviewLinks';
import DocumentationLink from 'components/support/DocumentationLink';

const _pageTilte = (activeBackend: ?AuthenticationBackend, returnString?: boolean) => {
  const pageName = 'Active Authentication Service';

  if (activeBackend) {
    const backendTitle = StringUtils.truncateWithEllipses(activeBackend.title, 30);

    if (returnString) {
      return `${pageName} - ${backendTitle}`;
    }

    return <>{pageName} - <i>{backendTitle}</i></>;
  }

  return pageName;
};

const useRedirectToAppropriatePage = (finishedLoading, activeBackend, backendsTotal) => {
  useEffect(() => {
    if (finishedLoading && !activeBackend && backendsTotal === 0) {
      history.push(Routes.SYSTEM.AUTHENTICATION.BACKENDS.CREATE);
    } else if (finishedLoading && !activeBackend && backendsTotal) {
      history.push(Routes.SYSTEM.AUTHENTICATION.BACKENDS.OVERVIEW);
    }
  }, [finishedLoading, activeBackend, backendsTotal]);
};

const AuthenticationPage = () => {
  const { finishedLoading, activeBackend, backendsTotal } = useActiveBackend();

  // Only display this page if there is an active backend
  // Otherwise redirect to correct page
  useRedirectToAppropriatePage(finishedLoading, activeBackend, backendsTotal);

  if (!finishedLoading) {
    return <Spinner />;
  }

  return (
    <DocumentTitle title={_pageTilte(activeBackend, true)}>
      <>
        <PageHeader title={_pageTilte(activeBackend)}
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

export default withParams(AuthenticationPage);

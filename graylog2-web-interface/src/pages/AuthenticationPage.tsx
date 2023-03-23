/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import * as React from 'react';
import { useEffect } from 'react';
import 'components/authentication/bindings'; // Bind all authentication plugins

import AuthenticationPageNavigation from 'components/authentication/AuthenticationPageNavigation';
import DocsHelper from 'util/DocsHelper';
import withParams from 'routing/withParams';
import StringUtils from 'util/StringUtils';
import type AuthenticationBackend from 'logic/authentication/AuthenticationBackend';
import Routes from 'routing/Routes';
import useActiveBackend from 'components/authentication/useActiveBackend';
import { PageHeader, Spinner, DocumentTitle } from 'components/common';
import BackendActionLinks from 'components/authentication/BackendActionLinks';
import BackendDetails from 'components/authentication/BackendDetails';
import useHistory from 'routing/useHistory';

const _pageTitle = (activeBackend: AuthenticationBackend | undefined | null, returnString?: boolean) => {
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

const useRedirectToAppropriatePage = (finishedLoading: boolean, activeBackend: AuthenticationBackend, backendsTotal: number) => {
  const history = useHistory();

  useEffect(() => {
    if (finishedLoading && !activeBackend && backendsTotal === 0) {
      history.push(Routes.SYSTEM.AUTHENTICATION.BACKENDS.CREATE);
    } else if (finishedLoading && !activeBackend && backendsTotal) {
      history.push(Routes.SYSTEM.AUTHENTICATION.BACKENDS.OVERVIEW);
    }
  }, [finishedLoading, activeBackend, backendsTotal, history]);
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
    <DocumentTitle title={_pageTitle(activeBackend, true)}>
      <AuthenticationPageNavigation />
      <PageHeader title={_pageTitle(activeBackend)}
                  actions={(
                    <BackendActionLinks activeBackend={activeBackend}
                                        finishedLoading={finishedLoading} />
                  )}
                  documentationLink={{
                    title: 'Authentication documentation',
                    path: DocsHelper.PAGES.USERS_ROLES,
                  }}>
        <span>Configure Graylog&apos;s authentication services of this Graylog cluster.</span>
      </PageHeader>

      {finishedLoading && activeBackend && (
        <BackendDetails authenticationBackend={activeBackend} />
      )}
    </DocumentTitle>
  );
};

export default withParams(AuthenticationPage);

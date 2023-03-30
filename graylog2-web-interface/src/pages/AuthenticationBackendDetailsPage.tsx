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
import { useState, useEffect } from 'react';

import AuthenticationPageNavigation from 'components/authentication/AuthenticationPageNavigation';
import withParams from 'routing/withParams';
import { LinkContainer } from 'components/common/router';
import 'components/authentication/bindings'; // Bind all authentication plugins
import DocsHelper from 'util/DocsHelper';
import StringUtils from 'util/StringUtils';
import AuthenticationDomain from 'domainActions/authentication/AuthenticationDomain';
import { Spinner, PageHeader, DocumentTitle } from 'components/common';
import BackendDetails from 'components/authentication/BackendDetails';
import Routes from 'routing/Routes';
import { Button } from 'components/bootstrap';
import type AuthenticationBackend from 'logic/authentication/AuthenticationBackend';

type Props = {
  params: {
    backendId: string,
  },
};

const _pageTitle = (authBackendTitle, returnString = false) => {
  const pageName = 'Authentication Service Details';
  const backendTitle = StringUtils.truncateWithEllipses(authBackendTitle, 30);

  if (returnString) {
    return `${pageName} - ${backendTitle}`;
  }

  return <>{pageName} - <i>{backendTitle}</i></>;
};

const AuthenticationBackendDetailsPage = ({ params: { backendId } }: Props) => {
  const [authBackend, setAuthBackend] = useState<AuthenticationBackend | undefined>();

  useEffect(() => {
    AuthenticationDomain.load(backendId).then((response) => setAuthBackend(response.backend));
  }, [backendId]);

  if (!authBackend) {
    return <Spinner />;
  }

  return (
    <DocumentTitle title={_pageTitle(authBackend.title, true)}>
      <AuthenticationPageNavigation />
      <PageHeader title={_pageTitle(authBackend.title)}
                  actions={(
                    <LinkContainer to={Routes.SYSTEM.AUTHENTICATION.BACKENDS.edit(authBackend?.id)}>
                      <Button bsStyle="success"
                              type="button">
                        Edit Service
                      </Button>
                    </LinkContainer>
                  )}
                  documentationLink={{
                    title: 'Authentication documentation',
                    path: DocsHelper.PAGES.USERS_ROLES,
                  }}>
        <span>Configure Graylog&apos;s authentication services of this Graylog cluster.</span>
      </PageHeader>
      <BackendDetails authenticationBackend={authBackend} />
    </DocumentTitle>
  );
};

export default withParams(AuthenticationBackendDetailsPage);

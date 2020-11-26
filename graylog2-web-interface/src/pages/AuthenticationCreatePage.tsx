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

import {} from 'components/authentication/bindings'; // Bind all authentication plugins
import AuthenticationOverviewLinks from 'components/authentication/AuthenticationOverviewLinks';
import GettingStarted from 'components/authentication/BackendCreate/GettingStarted';
import { DocumentTitle, PageHeader } from 'components/common';
import useActiveBackend from 'components/authentication/useActiveBackend';
import DocsHelper from 'util/DocsHelper';
import DocumentationLink from 'components/support/DocumentationLink';
import BackendActionLinks from 'components/authentication/BackendActionLinks';

const AuthenticationCreatePage = () => {
  const { finishedLoading, activeBackend } = useActiveBackend();

  return (
    <DocumentTitle title="Create Authentication Service">
      <PageHeader title="Create Authentication Service"
                  subactions={(
                    <BackendActionLinks activeBackend={activeBackend}
                                        finishedLoading={finishedLoading} />
                  )}>
        <span>Configure Graylog&apos;s authentication services of this Graylog cluster.</span>
        <span>Read more authentication in the <DocumentationLink page={DocsHelper.PAGES.USERS_ROLES}
                                                                 text="documentation" />.
        </span>
        <AuthenticationOverviewLinks />
      </PageHeader>

      <GettingStarted title="Create New Authentication Service" />
    </DocumentTitle>
  );
};

export default AuthenticationCreatePage;

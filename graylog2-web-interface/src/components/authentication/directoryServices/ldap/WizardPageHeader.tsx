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

import AuthenticationSubareaNavigation from 'components/authentication/AuthenticationSubareaNavigation';
import DocsHelper from 'util/DocsHelper';
import StringUtils from 'util/StringUtils';
import type { DirectoryServiceBackend } from 'logic/authentication/directoryServices/types';
import type { OktaBackend } from 'logic/authentication/okta/types';
import { PageHeader } from 'components/common';
import useActiveBackend from 'components/authentication/useActiveBackend';
import BackendActionLinks from 'components/authentication/BackendActionLinks';
import DocumentationLink from 'components/support/DocumentationLink';

type Props = {
  authenticationBackend?: DirectoryServiceBackend | OktaBackend,
  title?: string,
};

const _pageTitle = (authBackend, title) => {
  if (authBackend) {
    const backendTitle = StringUtils.truncateWithEllipses(authBackend.title, 30);

    return <>Edit Authentication Service - <i>{backendTitle}</i></>;
  }

  return title || 'Create LDAP Authentication Service';
};

const WizardPageHeader = ({ authenticationBackend: authBackend, title }: Props) => {
  const { finishedLoading, activeBackend } = useActiveBackend();
  const pageTitle = _pageTitle(authBackend, title);

  return (
    <>
      <AuthenticationSubareaNavigation />
      <PageHeader title={pageTitle}
                  subactions={(
                    <BackendActionLinks activeBackend={activeBackend}
                                        finishedLoading={finishedLoading} />
                  )}
                  documentationLink={{
                    title: 'Authentication documentation',
                    path: DocsHelper.PAGES.USERS_ROLES,
                  }}>
        <span>Configure Graylog&apos;s authentication services of this Graylog cluster.</span>
      </PageHeader>
    </>
  );
};

WizardPageHeader.defaultProps = {
  authenticationBackend: undefined,
  title: undefined,
};

export default WizardPageHeader;

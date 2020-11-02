// @flow strict
import * as React from 'react';

import AuthenticationOverviewLinks from 'components/authentication/AuthenticationOverviewLinks';
import { PageHeader } from 'components/common';
import useActiveBackend from 'components/authentication/useActiveBackend';
import DocumentationLink from 'components/support/DocumentationLink';
import BackendActionLinks from 'components/authentication/BackendActionLinks';
import DocsHelper from 'util/DocsHelper';
import StringUtils from 'util/StringUtils';
import type { DirectoryServiceBackend } from 'logic/authentication/directoryServices/types';

type Props = {
  authenticationBackend?: DirectoryServiceBackend,
};

const _pageTitle = (authBackend) => {
  if (authBackend) {
    const backendTitle = StringUtils.truncateWithEllipses(authBackend.title, 30);

    return <>Edit Authentication Service - <i>{backendTitle}</i></>;
  }

  return 'Create Active Directory Authentication Service';
};

const WizardPageHeader = ({ authenticationBackend: authBackend }: Props) => {
  const { finishedLoading, activeBackend } = useActiveBackend();
  const pageTitle = _pageTitle(authBackend);

  return (
    <PageHeader title={pageTitle}
                subactions={(
                  <BackendActionLinks activeBackend={activeBackend}
                                      finishedLoading={finishedLoading} />
                )}>
      <span>Configure Graylog&apos;s authentication services of this Graylog cluster.</span>
      <span>
        Read more authentication in the <DocumentationLink page={DocsHelper.PAGES.USERS_ROLES}
                                                           text="documentation" />.
      </span>
      <AuthenticationOverviewLinks />
    </PageHeader>
  );
};

WizardPageHeader.defaultProps = {
  authenticationBackend: undefined,
};

export default WizardPageHeader;

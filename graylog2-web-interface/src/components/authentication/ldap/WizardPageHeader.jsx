// @flow strict
import * as React from 'react';

import { PageHeader as CommonPageHeader } from 'components/common';
import { useActiveBackend } from 'components/authentication/hooks';
import DocumentationLink from 'components/support/DocumentationLink';
import BackendOverviewLinks from 'components/authentication/BackendOverviewLinks';
import DocsHelper from 'util/DocsHelper';
import StringUtils from 'util/StringUtils';
import type { LdapBackend } from 'logic/authentication/ldap/types';

type Props = {
  authenticationBackend?: LdapBackend,
};

const _pageTitle = (authBackend) => {
  if (authBackend) {
    const backendTitle = StringUtils.truncateWithEllipses(authBackend.title, 30);

    return <>Edit Authentication Service - <i>{backendTitle}</i></>;
  }

  return 'Create LDAP Authentication Service';
};

const WizardPageHeader = ({ authenticationBackend: authBackend }: Props) => {
  const { finishedLoading, activeBackend } = useActiveBackend();
  const pageTitle = _pageTitle(authBackend);

  return (
    <CommonPageHeader title={pageTitle}>
      <span>Configure Graylog&apos;s authentication services of this Graylog cluster.</span>
      <span>
        Read more authentication in the <DocumentationLink page={DocsHelper.PAGES.USERS_ROLES}
                                                           text="documentation" />.
      </span>

      <BackendOverviewLinks activeBackend={activeBackend}
                            finishedLoading={finishedLoading} />
    </CommonPageHeader>
  );
};

WizardPageHeader.defaultProps = {
  authenticationBackend: undefined,
};

export default WizardPageHeader;

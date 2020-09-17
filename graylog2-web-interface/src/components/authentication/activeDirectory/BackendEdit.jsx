// @flow strict
import * as React from 'react';

import DocsHelper from 'util/DocsHelper';
import { PageHeader, DocumentTitle } from 'components/common';
import DocumentationLink from 'components/support/DocumentationLink';
import BackendOverviewLinks from 'components/authentication/BackendOverviewLinks';
import AuthenticationDomain from 'domainActions/authentication/AuthenticationDomain';
import type { LdapBackend, LdapCreate } from 'logic/authentication/ldap/types';
import { useActiveBackend } from 'components/authentication/hooks';

import { HELP } from './BackendCreate';

import { prepareInitialValues } from '../ldap/BackendEdit';
import BackendWizard from '../BackendWizard';

type Props = {
  authenticationBackend: LdapBackend,
  initialStepKey: ?string,
};

const _optionalWizardProps = (initialStepKey: ?string) => {
  const props = {};

  if (initialStepKey) {
    props.initialStepKey = initialStepKey;
  }

  return props;
};

const BackendEdit = ({ authenticationBackend, initialStepKey }: Props) => {
  const { finishedLoading, activeBackend } = useActiveBackend();
  const initialValues = prepareInitialValues(authenticationBackend.config);
  const optionalProps = _optionalWizardProps(initialStepKey);
  const _handleSubmit = (payload: LdapCreate) => AuthenticationDomain.update(authenticationBackend.id,
    {
      ...payload,
      id: authenticationBackend.id,
    });

  return (
    <DocumentTitle title="Edit Active Directory Authentication Service">
      <PageHeader title="Edit Active Directory Authentication Service">
        <span>Configure Graylog&apos;s authentication services of this Graylog cluster.</span>
        <span>
          Read more authentication in the <DocumentationLink page={DocsHelper.PAGES.USERS_ROLES}
                                                             text="documentation" />.
        </span>

        <BackendOverviewLinks activeBackend={activeBackend}
                              finishedLoading={finishedLoading} />
      </PageHeader>

      <BackendWizard {...optionalProps}
                     initialValues={initialValues}
                     help={HELP}
                     onSubmit={_handleSubmit}
                     authServiceType={authenticationBackend.config.type}
                     editing />
    </DocumentTitle>
  );
};

export default BackendEdit;

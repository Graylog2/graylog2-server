// @flow strict
import * as React from 'react';

import AuthenticationDomain from 'domainActions/authentication/AuthenticationDomain';
import DocsHelper from 'util/DocsHelper';
import { PageHeader, DocumentTitle } from 'components/common';
import DocumentationLink from 'components/support/DocumentationLink';
import BackendOverviewLinks from 'components/authentication/BackendOverviewLinks';

import BackendWizard from '../BackendWizard';

export const HELP = {
  systemUsername: (
    <span>
      The username for the initial connection to the Active Directory server, e.g. <code>ldapbind@some.domain</code>.<br />
      This needs to match the <code>userPrincipalName</code> of that user.
    </span>
  ),
  systemPassword: 'The password for the initial connection to the Active Directory server.',
  userSearchBase: (
    <span>
      The base tree to limit the Active Directory search query to, e.g. <code>cn=users,dc=example,dc=com</code>.
    </span>
  ),
  userSearchPattern: (
    <span>
      For example <code className="text-nowrap">{'(&(objectClass=user)(sAMAccountName={0}))'}</code>.{' '}
      The string <code>{'{0}'}</code> will be replaced by the entered username.
    </span>
  ),
  displayNameAttribute: (
    <span>
      Which Active Directory attribute to use for the full name of the user in Graylog, e.g. <code>displayName</code>.<br />
      Try to load a test user using the form below, if you are unsure which attribute to use.
    </span>
  ),
  groupSearchBase: (
    <span>
      The base tree to limit the Active Directory group search query to, e.g. <code>cn=users,dc=example,dc=com</code>.
    </span>
  ),
  groupSearchPattern: (
    <span>
      The search pattern used to find groups in Active Directory for mapping to Graylog roles, e.g.{' '}
      <code className="text-nowrap">(objectClass=group)</code> or{' '}
      <code className="text-nowrap">(&amp;(objectClass=group)(cn=graylog*))</code>.
    </span>
  ),
  groupNameAttribute: (
    <span>Which Active Directory attribute to use for the full name of the group, usually <code>cn</code>.</span>
  ),
};

const BackendCreate = () => {
  return (
    <DocumentTitle title="Create Active Directory Authentication Services">
      <PageHeader title="Create Active Directory Services">
        <span>Configure Graylog&apos;s authentication services of this Graylog cluster.</span>
        <span>
          Read more authentication in the <DocumentationLink page={DocsHelper.PAGES.USERS_ROLES}
                                                             text="documentation"
                                                             help="HELP" />.
        </span>
        <BackendOverviewLinks />
      </PageHeader>
      <BackendWizard onSubmit={AuthenticationDomain.create} authServiceType="active-directory" />
    </DocumentTitle>
  );
};

export default BackendCreate;

// @flow strict
import * as React from 'react';
import styled from 'styled-components';
import { PluginStore } from 'graylog-web-plugin/plugin';
import { Formik } from 'formik';

import type { LdapCreate } from 'logic/authentication/ldap/types';

export type StepKeyType = 'group-synchronisation';
export const STEP_KEY: StepKeyType = 'group-synchronisation';

const Header = styled.h4`
  margin-bottom: 5px;
`;

const NoEnterpriseComponent = () => (
  <>
    <Header>No enterprise plugin found</Header>
    <p>To use the <b>Teams</b> functionality you need to install the Graylog <b>Enterprise</b> plugin.</p>
  </>
);

type Props = {
  formRef: React.Ref<typeof Formik>,
  onSubmitAll: () => void,
  prepareSubmitPayload: () => LdapCreate,
  submitAllError: ?React.Node,
  validateOnMount: boolean,
};

const GroupSyncStep = ({ onSubmitAll, prepareSubmitPayload, formRef, submitAllError, validateOnMount }: Props) => {
  const authGroupSyncPlugins = PluginStore.exports('authentication.enterprise.ldap.groupSync');

  if (!authGroupSyncPlugins || authGroupSyncPlugins.length <= 0) {
    return <NoEnterpriseComponent />;
  }

  const { components: { GroupSyncForm } } = authGroupSyncPlugins[0];

  return (
    <GroupSyncForm formRef={formRef}
                   onSubmitAll={onSubmitAll}
                   prepareSubmitPayload={prepareSubmitPayload}
                   submitAllError={submitAllError}
                   validateOnMount={validateOnMount} />
  );
};

export default GroupSyncStep;

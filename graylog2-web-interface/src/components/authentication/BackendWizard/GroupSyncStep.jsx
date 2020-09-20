// @flow strict
import * as React from 'react';
import styled from 'styled-components';
import { PluginStore } from 'graylog-web-plugin/plugin';
import { Formik } from 'formik';

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
  validateOnMount: boolean,
};

const GroupSyncStep = ({ onSubmitAll, formRef, validateOnMount }: Props) => {
  const authenticationPlugin = PluginStore.exports('authentication.groupSync');

  if (!authenticationPlugin || authenticationPlugin.length <= 0) {
    return <NoEnterpriseComponent />;
  }

  const { GroupSyncForm } = authenticationPlugin[0];

  return (
    <GroupSyncForm formRef={formRef}
                   onSubmitAll={onSubmitAll}
                   validateOnMount={validateOnMount} />
  );
};

export default GroupSyncStep;

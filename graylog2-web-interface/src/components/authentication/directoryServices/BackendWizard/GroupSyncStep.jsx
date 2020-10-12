// @flow strict
import * as React from 'react';
import styled from 'styled-components';
import { Formik } from 'formik';

import { getEnterpriseGroupSyncPlugin } from 'util/AuthenticationService';
import type { WizardSubmitPayload } from 'logic/authentication/directoryServices/types';

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
  onSubmitAll: () => Promise<void>,
  prepareSubmitPayload: () => WizardSubmitPayload,
  submitAllError: ?React.Node,
  validateOnMount: boolean,
};

const GroupSyncStep = ({ onSubmitAll, prepareSubmitPayload, formRef, submitAllError, validateOnMount }: Props) => {
  const enterpriseGroupSyncPlugin = getEnterpriseGroupSyncPlugin();

  if (!enterpriseGroupSyncPlugin) {
    return <NoEnterpriseComponent />;
  }

  const { components: { GroupSyncForm } } = enterpriseGroupSyncPlugin;

  return (
    <GroupSyncForm formRef={formRef}
                   onSubmitAll={onSubmitAll}
                   prepareSubmitPayload={prepareSubmitPayload}
                   submitAllError={submitAllError}
                   validateOnMount={validateOnMount} />
  );
};

export default GroupSyncStep;

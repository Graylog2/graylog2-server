// @flow strict
import * as React from 'react';
import { Formik } from 'formik';

import { getEnterpriseGroupSyncPlugin } from 'util/AuthenticationService';
import type { WizardSubmitPayload } from 'logic/authentication/directoryServices/types';
import { EnterprisePluginNotFound } from 'components/common';

export type StepKeyType = 'group-synchronisation';
export const STEP_KEY: StepKeyType = 'group-synchronisation';

export type Props = {
  formRef: React.Ref<typeof Formik>,
  onSubmitAll: (licenseIsValid?: boolean) => Promise<void>,
  help: { [inputName: string]: ?React.Node },
  prepareSubmitPayload: () => WizardSubmitPayload,
  submitAllError: ?React.Node,
  validateOnMount: boolean,
};

const GroupSyncStep = ({ onSubmitAll, prepareSubmitPayload, formRef, submitAllError, validateOnMount }: Props) => {
  const enterpriseGroupSyncPlugin = getEnterpriseGroupSyncPlugin();
  const GroupSyncForm = enterpriseGroupSyncPlugin?.components?.GroupSyncForm;

  if (!GroupSyncForm) {
    return <EnterprisePluginNotFound featureName="group synchronization" />;
  }

  return (
    <GroupSyncForm formRef={formRef}
                   onSubmitAll={onSubmitAll}
                   prepareSubmitPayload={prepareSubmitPayload}
                   submitAllError={submitAllError}
                   validateOnMount={validateOnMount} />
  );
};

export default GroupSyncStep;

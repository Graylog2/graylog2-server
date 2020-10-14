// @flow strict
import * as React from 'react';
import { Formik } from 'formik';

import type { WizardSubmitPayload } from 'logic/authentication/directoryServices/types';
import { EnterprisePluginNotFound } from 'components/common';

export type StepKeyType = 'group-synchronisation';
export const STEP_KEY: StepKeyType = 'group-synchronisation';

export type FormComponentProps = {
  formRef: React.Ref<typeof Formik>,
  onSubmitAll: (licenseIsValid?: boolean) => Promise<void>,
  prepareSubmitPayload: () => WizardSubmitPayload,
  submitAllError: ?React.Node,
  validateOnMount: boolean,
};

export type FromComponent = React.ComponentType<FormComponentProps>;

type Props = FormComponentProps & {
  formComponent: ?FromComponent,
};

const GroupSyncStep = ({ onSubmitAll, prepareSubmitPayload, formRef, formComponent: FormComponent, submitAllError, validateOnMount }: Props) => {
  if (!FormComponent) {
    return <EnterprisePluginNotFound featureName="group synchronization" />;
  }

  return (
    <FormComponent formRef={formRef}
                   onSubmitAll={onSubmitAll}
                   prepareSubmitPayload={prepareSubmitPayload}
                   submitAllError={submitAllError}
                   validateOnMount={validateOnMount} />
  );
};

export default GroupSyncStep;

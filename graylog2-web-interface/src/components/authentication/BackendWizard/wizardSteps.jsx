// @flow strict
import * as React from 'react';
import { Formik } from 'formik';

import ServerConfigStep from './ServerConfigStep';
import UserSyncStep from './UserSyncStep';
import GroupSyncStep from './GroupSyncStep';
import StepTitleWarning from './StepTitleWarning';

type Props = {
  formRefs: {
    serverConfig: React.ElementRef<typeof Formik | null>,
    userSync: React.ElementRef<typeof Formik | null>,
    groupSync: React.ElementRef<typeof Formik | null>,
  },
  invalidStepKeys: Array<string>,
  handleSubmitAll: () => void,
  setActiveStepKey: (stepKey: string)=> void,
};

const wizardSteps = ({ invalidStepKeys, formRefs, handleSubmitAll, setActiveStepKey }: Props) => [
  {
    key: 'serverConfig',
    title: (
      <>
        <StepTitleWarning invalidStepKeys={invalidStepKeys} stepKey="serverConfig" />
        Server Configuration
      </>
    ),
    component: (
      <ServerConfigStep onSubmit={() => setActiveStepKey('userSync')}
                        onSubmitAll={handleSubmitAll}
                        validateOnMount={invalidStepKeys.includes('serverConfig')}
                        formRef={formRefs.serverConfig} />
    ),
  },
  {
    key: 'userSync',
    title: (
      <>
        <StepTitleWarning invalidStepKeys={invalidStepKeys} stepKey="userSync" />
        User Synchronisation
      </>
    ),
    component: (
      <UserSyncStep onSubmit={() => setActiveStepKey('groupSync')}
                    validateOnMount={invalidStepKeys.includes('userSync')}
                    formRef={formRefs.userSync}
                    onSubmitAll={handleSubmitAll} />
    ),
  },
  {
    key: 'groupSync',
    title: (
      <>
        <StepTitleWarning invalidStepKeys={invalidStepKeys} stepKey="groupSync" />
        Group Synchronisation (Opt.)
      </>
    ),
    component: (
      <GroupSyncStep validateOnMount={invalidStepKeys.includes('groupSync')}
                     formRef={formRefs.groupSync}
                     onSubmitAll={handleSubmitAll} />
    ),
  },
];

export default wizardSteps;

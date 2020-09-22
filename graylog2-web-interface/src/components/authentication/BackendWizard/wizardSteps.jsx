// @flow strict
import * as React from 'react';
import { Formik } from 'formik';

import ServerConfigStep, { STEP_KEY as SERVER_CONFIG_KEY, type StepKeyType as ServerConfigKey } from './ServerConfigStep';
import UserSyncStep, { STEP_KEY as USER_SYNC_KEY, type StepKeyType as UserSyncKey } from './UserSyncStep';
import GroupSyncStep, { STEP_KEY as GROUP_SYNC_KEY, type StepKeyType as GroupSyncKey } from './GroupSyncStep';
import StepTitleWarning from './StepTitleWarning';

type Props = {
  formRefs: {
    [ServerConfigKey | UserSyncKey | GroupSyncKey]: React.Ref<typeof Formik>,
  },
  handleSubmitAll: () => void,
  invalidStepKeys: Array<string>,
  setActiveStepKey: (stepKey: string)=> void,
  submitAllError: string,
};

const wizardSteps = ({ formRefs, handleSubmitAll, invalidStepKeys, setActiveStepKey, submitAllError }: Props) => [
  {
    key: SERVER_CONFIG_KEY,
    title: (
      <>
        <StepTitleWarning invalidStepKeys={invalidStepKeys} stepKey={SERVER_CONFIG_KEY} />
        Server Configuration
      </>
    ),
    component: (
      <ServerConfigStep formRef={formRefs[SERVER_CONFIG_KEY]}
                        onSubmit={() => setActiveStepKey(USER_SYNC_KEY)}
                        onSubmitAll={handleSubmitAll}
                        submitAllError={submitAllError}
                        validateOnMount={invalidStepKeys.includes(SERVER_CONFIG_KEY)} />
    ),
  },
  {
    key: USER_SYNC_KEY,
    title: (
      <>
        <StepTitleWarning invalidStepKeys={invalidStepKeys} stepKey={USER_SYNC_KEY} />
        User Synchronisation
      </>
    ),
    component: (
      <UserSyncStep formRef={formRefs[USER_SYNC_KEY]}
                    onSubmit={() => setActiveStepKey(GROUP_SYNC_KEY)}
                    onSubmitAll={handleSubmitAll}
                    submitAllError={submitAllError}
                    validateOnMount={invalidStepKeys.includes(USER_SYNC_KEY)} />
    ),
  },
  {
    key: GROUP_SYNC_KEY,
    title: (
      <>
        <StepTitleWarning invalidStepKeys={invalidStepKeys} stepKey={GROUP_SYNC_KEY} />
        Group Synchronisation (Opt.)
      </>
    ),
    component: (
      <GroupSyncStep formRef={formRefs[GROUP_SYNC_KEY]}
                     onSubmitAll={handleSubmitAll}
                     submitAllError={submitAllError}
                     validateOnMount={invalidStepKeys.includes(GROUP_SYNC_KEY)} />
    ),
  },
];

export default wizardSteps;

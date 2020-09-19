// @flow strict
import * as React from 'react';
import { Formik } from 'formik';

import ServerConfigStep, { STEP_KEY as SERVER_CONFIG_KEY, type StepKeyType as ServerConfigKey } from './ServerConfigStep';
import UserSyncStep, { STEP_KEY as USER_SYNC_KEY, type StepKeyType as UserSyncKey } from './UserSyncStep';
import GroupSyncStep, { STEP_KEY as GROUP_SYNC_KEY, type StepKeyType as GroupSyncKey } from './GroupSyncStep';
import StepTitleWarning from './StepTitleWarning';

type Props = {
  formRefs: {
    [ServerConfigKey | UserSyncKey | GroupSyncKey]: React.ElementRef<typeof Formik | null>,
  },
  invalidStepKeys: Array<string>,
  handleSubmitAll: () => void,
  setActiveStepKey: (stepKey: string)=> void,
};

const wizardSteps = ({ invalidStepKeys, formRefs, handleSubmitAll, setActiveStepKey }: Props) => [
  {
    key: SERVER_CONFIG_KEY,
    title: (
      <>
        <StepTitleWarning invalidStepKeys={invalidStepKeys} stepKey={SERVER_CONFIG_KEY} />
        Server Configuration
      </>
    ),
    component: (
      <ServerConfigStep onSubmit={() => setActiveStepKey(USER_SYNC_KEY)}
                        onSubmitAll={handleSubmitAll}
                        validateOnMount={invalidStepKeys.includes(SERVER_CONFIG_KEY)}
                        formRef={formRefs[SERVER_CONFIG_KEY]} />
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
      <UserSyncStep onSubmit={() => setActiveStepKey(GROUP_SYNC_KEY)}
                    validateOnMount={invalidStepKeys.includes(USER_SYNC_KEY)}
                    formRef={formRefs[USER_SYNC_KEY]}
                    onSubmitAll={handleSubmitAll} />
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
      <GroupSyncStep validateOnMount={invalidStepKeys.includes(GROUP_SYNC_KEY)}
                     formRef={formRefs[GROUP_SYNC_KEY]}
                     onSubmitAll={handleSubmitAll} />
    ),
  },
];

export default wizardSteps;

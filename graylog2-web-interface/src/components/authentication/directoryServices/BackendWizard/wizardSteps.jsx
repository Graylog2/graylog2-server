// @flow strict
import * as React from 'react';
import * as Immutable from 'immutable';
import { Formik } from 'formik';

import type { WizardSubmitPayload } from 'logic/authentication/directoryServices/types';
import Role from 'logic/roles/Role';

import ServerConfigStep, { STEP_KEY as SERVER_CONFIG_KEY, type StepKeyType as ServerConfigKey } from './ServerConfigStep';
import UserSyncStep, { STEP_KEY as USER_SYNC_KEY, type StepKeyType as UserSyncKey } from './UserSyncStep';
import GroupSyncStep, { STEP_KEY as GROUP_SYNC_KEY, type StepKeyType as GroupSyncKey } from './GroupSyncStep';
import StepTitleWarning from './StepTitleWarning';

type Props = {
  formRefs: {
    [ServerConfigKey | UserSyncKey | GroupSyncKey]: React.Ref<typeof Formik>,
  },
  excludedFields: { [inputName: string]: boolean },
  handleSubmitAll: (shouldUpdateGroupSync?: boolean) => Promise<void>,
  help: { [inputName: string]: ?React.Node },
  invalidStepKeys: Array<string>,
  prepareSubmitPayload: () => WizardSubmitPayload,
  roles: Immutable.List<Role>,
  setActiveStepKey: (stepKey: string) => void,
  submitAllError: ?React.Node,
};

const wizardSteps = ({
  formRefs,
  handleSubmitAll,
  help,
  excludedFields,
  invalidStepKeys,
  prepareSubmitPayload,
  roles,
  setActiveStepKey,
  submitAllError,
}: Props) => [
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
                        help={help}
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
        User Synchronization
      </>
    ),
    component: (
      <UserSyncStep formRef={formRefs[USER_SYNC_KEY]}
                    help={help}
                    excludedFields={excludedFields}
                    onSubmit={() => setActiveStepKey(GROUP_SYNC_KEY)}
                    onSubmitAll={handleSubmitAll}
                    roles={roles}
                    submitAllError={submitAllError}
                    validateOnMount={invalidStepKeys.includes(USER_SYNC_KEY)} />
    ),
  },
  {
    key: GROUP_SYNC_KEY,
    title: (
      <>
        <StepTitleWarning invalidStepKeys={invalidStepKeys} stepKey={GROUP_SYNC_KEY} />
        Group Synchronization (Opt.)
      </>
    ),
    component: (
      <GroupSyncStep formRef={formRefs[GROUP_SYNC_KEY]}
                     help={help}
                     excludedFields={excludedFields}
                     onSubmitAll={handleSubmitAll}
                     prepareSubmitPayload={prepareSubmitPayload}
                     roles={roles}
                     submitAllError={submitAllError}
                     validateOnMount={invalidStepKeys.includes(GROUP_SYNC_KEY)} />
    ),
  },
];

export default wizardSteps;

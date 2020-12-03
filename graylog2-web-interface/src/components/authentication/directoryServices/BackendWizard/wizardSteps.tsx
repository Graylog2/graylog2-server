/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import * as React from 'react';
import * as Immutable from 'immutable';
import { FormikProps } from 'formik';

import { WizardSubmitPayload } from 'logic/authentication/directoryServices/types';
import Role from 'logic/roles/Role';
import { StepKey } from 'components/common/Wizard';

import { WizardFormValues } from './BackendWizardContext';
import ServerConfigStep, { STEP_KEY as SERVER_CONFIG_KEY } from './ServerConfigStep';
import UserSyncStep, { STEP_KEY as USER_SYNC_KEY } from './UserSyncStep';
import GroupSyncStep, { STEP_KEY as GROUP_SYNC_KEY } from './GroupSyncStep';
import StepTitleWarning from './StepTitleWarning';

type Props = {
  formRefs: Record<typeof SERVER_CONFIG_KEY | typeof USER_SYNC_KEY | typeof GROUP_SYNC_KEY, React.Ref<FormikProps<WizardFormValues>>>,
  excludedFields: { [inputName: string]: boolean },
  handleSubmitAll: (shouldUpdateGroupSync?: boolean) => Promise<void>,
  help: { [inputName: string]: React.ReactElement | string | null | undefined },
  invalidStepKeys: Array<StepKey>,
  prepareSubmitPayload: (fromValues: WizardFormValues | null | undefined) => WizardSubmitPayload,
  roles: Immutable.List<Role>,
  setActiveStepKey: (stepKey: string) => void,
  submitAllError: React.ReactNode | null | undefined,
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

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
import type * as Immutable from 'immutable';
import type { FormikProps } from 'formik';

import { Row, Col, Button, ButtonToolbar } from 'components/bootstrap';
import { EnterprisePluginNotFound } from 'components/common';
import type Role from 'logic/roles/Role';
import { getEnterpriseGroupSyncPlugin } from 'util/AuthenticationService';
import type { WizardSubmitPayload } from 'logic/authentication/directoryServices/types';
import { getPathnameWithoutId } from 'util/URLUtils';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import useLocation from 'routing/useLocation';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

import type { WizardFormValues } from './BackendWizardContext';

export const STEP_KEY = 'group-synchronization';

export type Props = {
  formRef: React.Ref<FormikProps<WizardFormValues>>,
  onSubmitAll: (shouldUpdateGroupSync?: boolean) => Promise<void>,
  help: { [inputName: string]: React.ReactElement | string | null | undefined },
  excludedFields: { [inputName: string]: boolean },
  prepareSubmitPayload: (fromValues: WizardFormValues | null | undefined) => WizardSubmitPayload,
  roles: Immutable.List<Role>,
  submitAllError: React.ReactNode | null | undefined,
  validateOnMount: boolean,
};

const GroupSyncStep = ({
  onSubmitAll,
  prepareSubmitPayload,
  formRef,
  submitAllError,
  validateOnMount,
  roles,
  help,
  excludedFields,
}: Props) => {
  const { pathname } = useLocation();
  const sendTelemetry = useSendTelemetry();

  const enterpriseGroupSyncPlugin = getEnterpriseGroupSyncPlugin();
  const GroupSyncForm = enterpriseGroupSyncPlugin?.components?.GroupSyncForm;

  if (!GroupSyncForm) {
    return (
      <>
        <Row>
          <Col xs={12}>
            <EnterprisePluginNotFound featureName="group synchronization" />
          </Col>
        </Row>
        <ButtonToolbar className="pull-right">
          <Button bsStyle="primary"
                  onClick={() => {
                    sendTelemetry(TELEMETRY_EVENT_TYPE.AUTHENTICATION.DIRECTORY_GROUP_SYNC_SAVE_CLICKED, {
                      app_pathname: getPathnameWithoutId(pathname),
                      app_section: 'directory-service',
                      app_action_value: 'groupsync-save',
                    });

                    onSubmitAll(false);
                  }}>
            Finish & Save Service
          </Button>
        </ButtonToolbar>
      </>
    );
  }

  return (
    <GroupSyncForm formRef={formRef}
                   help={help}
                   excludedFields={excludedFields}
                   onSubmitAll={onSubmitAll}
                   prepareSubmitPayload={prepareSubmitPayload}
                   roles={roles}
                   submitAllError={submitAllError}
                   validateOnMount={validateOnMount} />
  );
};

export default GroupSyncStep;

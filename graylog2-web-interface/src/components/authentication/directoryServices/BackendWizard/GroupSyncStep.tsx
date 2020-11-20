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
// @flow strict
import * as React from 'react';
import * as Immutable from 'immutable';
import { Formik } from 'formik';

import { Row, Col, Button, ButtonToolbar } from 'components/graylog';
import Role from 'logic/roles/Role';
import { getEnterpriseGroupSyncPlugin } from 'util/AuthenticationService';
import type { WizardSubmitPayload } from 'logic/authentication/directoryServices/types';
import { EnterprisePluginNotFound } from 'components/common';

export type StepKeyType = 'group-synchronization';
export const STEP_KEY: StepKeyType = 'group-synchronization';

export type Props = {
  formRef: React.Ref<typeof Formik>,
  onSubmitAll: (shouldUpdateGroupSync?: boolean) => Promise<void>,
  help: { [inputName: string]: ?React.Node },
  excludedFields: { [inputName: string]: boolean },
  prepareSubmitPayload: () => WizardSubmitPayload,
  roles: Immutable.List<Role>,
  submitAllError: ?React.Node,
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
                  onClick={() => onSubmitAll(false)}>
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

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
import { Formik, Form } from 'formik';

import { Input } from 'components/bootstrap';

import type { RemoteReindexRequest } from '../../hooks/useRemoteReindexMigrationStatus';
import type { MigrationStepComponentProps } from '../../Types';
import MigrationStepTriggerButtonToolbar from '../common/MigrationStepTriggerButtonToolbar';

const MigrateExistingData = ({ currentStep, onTriggerStep }: MigrationStepComponentProps) => {
  const initialValues: RemoteReindexRequest = {
    hostname: '',
    user: '',
    password: '',
    synchronous: false,
    indices: [],
  };

  return (
    <Formik initialValues={initialValues} onSubmit={() => {}}>
      {({
        values,
        handleChange,
      }) => (
        <Form role="form">
          <Input id="hostname"
                 name="hostname"
                 label="Cluster URI"
                 type="text"
                 value={values.hostname}
                 onChange={handleChange}
                 required />
          <Input id="user"
                 name="user"
                 label="Username"
                 type="text"
                 value={values.user}
                 onChange={handleChange} />
          <Input id="password"
                 name="password"
                 label="Password"
                 type="password"
                 value={values.password}
                 onChange={handleChange} />
          <MigrationStepTriggerButtonToolbar nextSteps={currentStep.next_steps} onTriggerStep={onTriggerStep} args={values} />
        </Form>
      )}
    </Formik>
  );
};

export default MigrateExistingData;

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
import { useState } from 'react';
import { Formik, Form } from 'formik';

import { Alert, Input } from 'components/bootstrap';

import type { RemoteReindexRequest } from '../../hooks/useRemoteReindexMigrationStatus';
import type { MigrationActions, MigrationStepComponentProps, StepArgs } from '../../Types';
import MigrationStepTriggerButtonToolbar from '../common/MigrationStepTriggerButtonToolbar';

export type RemoteReindexCheckConnection = {
  indices: string[],
  error: any,
}

const MigrateExistingData = ({ currentStep, onTriggerStep }: MigrationStepComponentProps) => {
  const [nextSteps, setNextSteps] = useState<MigrationActions[]>(['CHECK_REMOTE_INDEXER_CONNECTION']);
  const [errorMessage, setErrrorMessage] = useState<string|null>(null);
  const [indices, setIndices] = useState<string[]|undefined>(undefined);

  const handleTriggerNextStep = async (step: MigrationActions, args?: StepArgs) => {
    setErrrorMessage(null);
    return onTriggerStep(step, args).then((data) => {
      if (step === 'CHECK_REMOTE_INDEXER_CONNECTION') {
        const checkConnectionResult = data?.response as RemoteReindexCheckConnection;
        setIndices(checkConnectionResult?.indices)
      }

      return data;
    }).catch((error) => setErrrorMessage(error?.message))
  };


  const initialValues: RemoteReindexRequest = {
    hostname: 'http://localhost:9201/',
    user: '',
    password: '',
    synchronous: false,
    indices: [],
  };

  // first only show checkConnection then save a state that was successfull, reset that if any field was changed
  // you can show the list of indicies with checkboxes selected then show the migrate button

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
          {indices?.map((index) => (
            <Input type="checkbox"
                   key={index}
                   name={index}
                   id={index}
                   label={index} />
          ))}
          {errorMessage && <Alert bsStyle="danger">{errorMessage}</Alert>}
          <MigrationStepTriggerButtonToolbar nextSteps={nextSteps || currentStep.next_steps} onTriggerStep={handleTriggerNextStep} args={values} />
        </Form>
      )}
    </Formik>
  );
};

export default MigrateExistingData;

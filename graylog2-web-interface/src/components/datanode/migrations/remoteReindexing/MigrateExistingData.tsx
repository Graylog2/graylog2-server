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
import type { FormikErrors } from 'formik';

import { Alert, Input } from 'components/bootstrap';

import type { RemoteReindexRequest } from '../../hooks/useRemoteReindexMigrationStatus';
import type { MigrationActions, MigrationState, MigrationStepComponentProps, StepArgs } from '../../Types';
import MigrationStepTriggerButtonToolbar from '../common/MigrationStepTriggerButtonToolbar';

export type RemoteReindexCheckConnection = {
  indices: string[],
  error: any,
}

const MigrateExistingData = ({ currentStep, onTriggerStep }: MigrationStepComponentProps) => {
  const [nextSteps, setNextSteps] = useState<MigrationActions[]>(['CHECK_REMOTE_INDEXER_CONNECTION']);
  const [errorMessage, setErrrorMessage] = useState<string|null>(null);
  const [availableIndices, setAvailableIndices] = useState<string[]>([]);
  const [selectedIndices, setSelectedIndices] = useState<string[]>([]);

  const handleTriggerNextStep = async (step: MigrationActions, args?: StepArgs) => {
    setErrrorMessage(null);

    return onTriggerStep(step, args).then((data) => {
      if (step === 'CHECK_REMOTE_INDEXER_CONNECTION') {
        const checkConnectionResult = data?.response as RemoteReindexCheckConnection;

        if (checkConnectionResult?.indices?.length) {
          setAvailableIndices(checkConnectionResult.indices);
          setSelectedIndices(checkConnectionResult.indices);
          setNextSteps(currentStep.next_steps.filter((next_step) => next_step === 'START_REMOTE_REINDEX_MIGRATION'));
        } else {
          if (checkConnectionResult?.error) {
            setErrrorMessage(checkConnectionResult.error);
          } else {
            setErrrorMessage('No available index has been found for remote reindex migration.');
          }
        }
      }

      return data;
    }).catch((error) => {
      setErrrorMessage(error?.message);

      return {} as MigrationState;
    });
  };

  const resetConnectionCheck = () => {
    setErrrorMessage(null);
    setAvailableIndices([]);
    setSelectedIndices([]);
    setNextSteps(currentStep.next_steps.filter((step) => step === 'CHECK_REMOTE_INDEXER_CONNECTION'));
  };

  const handleChange = async (e: React.ChangeEvent<any>, callback: (field: string, value: any, shouldValidate?: boolean) => Promise<void | FormikErrors<RemoteReindexRequest>>) => {
    await callback(e.target.name, e.target.value);
    resetConnectionCheck();
  };

  const handleSelectIndices = (indexToToggle: string) => {
    if (selectedIndices.includes(indexToToggle)) {
      setSelectedIndices(selectedIndices.filter((index) => index !== indexToToggle));
    } else {
      setSelectedIndices([...selectedIndices, indexToToggle]);
    }
  };

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
        setFieldValue,
      }) => (
        <Form role="form">
          <Input id="hostname"
                 name="hostname"
                 label="Cluster URI"
                 type="text"
                 value={values.hostname}
                 onChange={(e) => handleChange(e, setFieldValue)}
                 required />
          <Input id="user"
                 name="user"
                 label="Username"
                 type="text"
                 value={values.user}
                 onChange={(e) => handleChange(e, setFieldValue)} />
          <Input id="password"
                 name="password"
                 label="Password"
                 type="password"
                 value={values.password}
                 onChange={(e) => handleChange(e, setFieldValue)} />
          {(availableIndices.length > 0) && (
            <Alert title="Valid connection" bsStyle="success">
              These are the available indices for the remote reindex migration:
              {availableIndices.map((index) => (
                <Input type="checkbox"
                       key={index}
                       name={index}
                       id={index}
                       label={index}
                       checked={selectedIndices.includes(index)}
                       onChange={() => handleSelectIndices(index)} />
              ))}
            </Alert>
          )}
          {errorMessage && (
            <Alert bsStyle="danger">{errorMessage}</Alert>
          )}
          <MigrationStepTriggerButtonToolbar nextSteps={nextSteps || currentStep.next_steps} onTriggerStep={handleTriggerNextStep} args={{ ...values, indices: selectedIndices } as RemoteReindexRequest} />
        </Form>
      )}
    </Formik>
  );
};

export default MigrateExistingData;

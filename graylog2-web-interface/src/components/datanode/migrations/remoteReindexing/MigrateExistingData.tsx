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
import styled from 'styled-components';

import { Alert, Input, Row, Col } from 'components/bootstrap';
import { SearchForm, Spinner } from 'components/common';
import { getValueFromInput } from 'util/FormsUtils';

import type { RemoteReindexRequest } from '../../hooks/useRemoteReindexMigrationStatus';
import type { MigrationActions, MigrationState, MigrationStepComponentProps, StepArgs } from '../../Types';
import MigrationStepTriggerButtonToolbar from '../common/MigrationStepTriggerButtonToolbar';

const DEFAULT_THREADS_COUNT = 4;

const IndicesContainer = styled.div`
  max-height: 300px;
  overflow-y: scroll;
  overflow: -moz-scrollbars-vertical;
  -ms-overflow-y: scroll;
`;

const SearchContainer = styled.div`
  margin-top: 12px;
`;

export type RemoteReindexCheckConnection = {
  indices: string[],
  error: any,
}

const MigrateExistingData = ({ currentStep, onTriggerStep, hideActions }: MigrationStepComponentProps) => {
  const [nextSteps, setNextSteps] = useState<MigrationActions[]>(['CHECK_REMOTE_INDEXER_CONNECTION']);
  const [errorMessage, setErrrorMessage] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [availableIndices, setAvailableIndices] = useState<string[]>([]);
  const [selectedIndices, setSelectedIndices] = useState<string[]>([]);
  const [queryIndex, setQueryIndex] = useState<string>('');

  const handleConnectionCheck = (step: MigrationActions, data: MigrationState) => {
    if (step === 'CHECK_REMOTE_INDEXER_CONNECTION') {
      const checkConnectionResult = data?.response as RemoteReindexCheckConnection;

      if (checkConnectionResult?.indices?.length) {
        setAvailableIndices(checkConnectionResult.indices);
        setSelectedIndices(checkConnectionResult.indices);
        setNextSteps(currentStep.next_steps.filter((next_step) => next_step === 'START_REMOTE_REINDEX_MIGRATION'));
      } else if (checkConnectionResult?.error) {
        setErrrorMessage(checkConnectionResult.error);
      } else {
        setErrrorMessage('No available index has been found for remote reindex migration.');
      }
    }
  };

  const handleTriggerNextStep = async (step: MigrationActions, args?: StepArgs) => {
    setIsLoading(true);
    setErrrorMessage(null);

    return onTriggerStep(step, args).then((data) => {
      handleConnectionCheck(step, data);

      return data;
    }).catch((error) => {
      setErrrorMessage(error?.message);

      return {} as MigrationState;
    }).finally(() => setIsLoading(false));
  };

  const resetConnectionCheck = () => {
    setErrrorMessage(null);
    setIsLoading(false);
    setAvailableIndices([]);
    setSelectedIndices([]);
    setNextSteps(currentStep.next_steps.filter((step) => step === 'CHECK_REMOTE_INDEXER_CONNECTION'));
  };

  const handleChange = async (e: React.ChangeEvent<any>, callback: (field: string, value: any, shouldValidate?: boolean) => Promise<void | FormikErrors<RemoteReindexRequest>>) => {
    let value;
    value = getValueFromInput(e.target);

    if (e.target.name === 'threads') {
      value = (value || 0) < 1 ? DEFAULT_THREADS_COUNT : value;
    }

    await callback(e.target.name, value);

    resetConnectionCheck();
  };

  const handleCheckboxChange = async (e: React.ChangeEvent<any>, callback: (field: string, value: any, shouldValidate?: boolean) => Promise<void | FormikErrors<RemoteReindexRequest>>) => {
    await callback(e.target.name, e.target.checked);
    resetConnectionCheck();
  };

  const handleSelectIndices = (indexToToggle: string) => {
    if (selectedIndices.includes(indexToToggle)) {
      setSelectedIndices(selectedIndices.filter((index) => index !== indexToToggle));
    } else {
      setSelectedIndices([...selectedIndices, indexToToggle]);
    }
  };

  const filteredIndices = queryIndex ? availableIndices.filter((index) => index.includes(queryIndex)) : availableIndices;
  const filteredSelectedIndices = selectedIndices.filter((index) => filteredIndices.includes(index));
  const areAllIndicesSelected = filteredSelectedIndices.length === filteredIndices.length;

  const initialValues: RemoteReindexRequest = {
    allowlist: '',
    hostname: '',
    user: '',
    password: '',
    synchronous: false,
    indices: [],
    threads: DEFAULT_THREADS_COUNT,
    trust_unknown_certs: false,
  };

  return (
    <Formik initialValues={initialValues}
            onSubmit={() => {
            }}>
      {({
        values,
        setFieldValue,
      }) => (
        <Form role="form">
          <Input id="hostname"
                 name="hostname"
                 label="Hostname"
                 help="URI of the host to call the remote reindexing command against (http://example:9200)"
                 placeholder="http://example:9200/"
                 type="text"
                 disabled={isLoading}
                 value={values.hostname}
                 onChange={(e) => handleChange(e, setFieldValue)}
                 required />
          <Row>
            <Col md={6}>
              <Input id="user"
                     name="user"
                     label="Username"
                     type="text"
                     disabled={isLoading}
                     value={values.user}
                     onChange={(e) => handleChange(e, setFieldValue)} />
            </Col>
            <Col md={6}>
              <Input id="password"
                     name="password"
                     label="Password"
                     type="password"
                     disabled={isLoading}
                     value={values.password}
                     onChange={(e) => handleChange(e, setFieldValue)} />
            </Col>
          </Row>
          <Input id="allowlist"
                 name="allowlist"
                 label="Allowlist"
                 help="Allowlist of all machines in the old cluster (example:9200,example:9201,example:9202 or Regular expression)"
                 placeholder="example:9200,example:9201,example:9202 or Regular expression"
                 type="text"
                 disabled={isLoading}
                 value={values.allowlist}
                 onChange={(e) => handleChange(e, setFieldValue)}
                 required />
          <Input id="threads"
                 name="threads"
                 label="Threads count"
                 help="Threads count defines how many indices will be migrated in parallel (minimum 1, default 4)"
                 type="number"
                 min={1}
                 step={1}
                 disabled={isLoading}
                 value={values.threads}
                 onChange={(e) => handleChange(e, setFieldValue)} />
          <Input id="trust_unknown_certs"
                 name="trust_unknown_certs"
                 label="Trust unknown certificates"
                 help="Trust all certificates of the remote host during the migration process."
                 type="checkbox"
                 disabled={isLoading}
                 checked={values.trust_unknown_certs}
                 onChange={(e) => handleCheckboxChange(e, setFieldValue)}
                 required />
          {(availableIndices.length > 0) && (
            <Alert title="Valid connection" bsStyle="success">
              Below are the available indices for the remote reindex migration, <b>{filteredSelectedIndices.length}/{availableIndices.length}</b> are selected.
              <SearchContainer>
                <SearchForm onSearch={setQueryIndex}
                            query={queryIndex} />
              </SearchContainer>
              {(filteredIndices.length === 0) ? 'No indices have been found' : (
                <Input type="checkbox"
                       formGroupClassName=""
                       label={<b>{areAllIndicesSelected ? 'Unselect all' : 'Select all'}</b>}
                       disabled={isLoading}
                       checked={areAllIndicesSelected}
                       onChange={() => {
                         if (areAllIndicesSelected) {
                           setSelectedIndices([]);
                         } else {
                           setSelectedIndices(filteredIndices);
                         }
                       }} />
              )}
              <IndicesContainer>
                {filteredIndices.map((index) => (
                  <Input type="checkbox"
                         key={index}
                         name={index}
                         id={index}
                         label={index}
                         disabled={isLoading}
                         checked={filteredSelectedIndices.includes(index)}
                         onChange={() => handleSelectIndices(index)} />
                ))}
              </IndicesContainer>
            </Alert>
          )}
          {errorMessage && (
            <Alert bsStyle="danger">{errorMessage}</Alert>
          )}
          {isLoading ? (
            <Spinner />
          ) : (
            <MigrationStepTriggerButtonToolbar hidden={hideActions}
                                               nextSteps={nextSteps || currentStep.next_steps}
                                               onTriggerStep={handleTriggerNextStep}
                                               args={{ ...values, indices: filteredSelectedIndices } as RemoteReindexRequest} />
          )}
        </Form>
      )}
    </Formik>
  );
};

export default MigrateExistingData;

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
import type { FormikHelpers } from 'formik';
import { Formik, Form } from 'formik';

import { Input, Button, ButtonToolbar } from 'components/bootstrap';

import type { RemoteReindexRequest } from '../../hooks/useRemoteReindexMigrationStatus';
import useRemoteReindexMigrationStatus, { remoteReindex } from '../../hooks/useRemoteReindexMigrationStatus';

type Props = {
  onStepComplete: () => void,
};

const MigrationWithDowntimeQuestion = ({ onStepComplete }: Props) => {
  const migrationID = '';
  useRemoteReindexMigrationStatus(migrationID);

  const initialValues: RemoteReindexRequest = {
    hostname: '',
    user: '',
    password: '',
    synchronous: false,
    indices: [],
  };

  const onSubmit = (values: RemoteReindexRequest, _formikHelpers: FormikHelpers<RemoteReindexRequest>) => {
    remoteReindex(values);
  };

  return (
    <Formik initialValues={initialValues} onSubmit={onSubmit}>
      {({
        values,
        handleChange,
        isSubmitting,
      }) => (
        <Form role="form">
          <Input id="hostname"
                 name="hostname"
                 label="Hostname"
                 type="text"
                 value={values.hostname}
                 onChange={handleChange}
                 required />
          <Input id="user"
                 name="user"
                 label="User"
                 type="text"
                 value={values.user}
                 onChange={handleChange} />
          <Input id="password"
                 name="password"
                 label="Password"
                 type="password"
                 value={values.password}
                 onChange={handleChange} />
          <ButtonToolbar>
            <Button type="submit"
                    disabled={isSubmitting}
                    bsStyle="primary"
                    bsSize="small"
                    onClick={() => onStepComplete()}>
              {isSubmitting ? 'Loading...' : 'Next'}
            </Button>
          </ButtonToolbar>
        </Form>
      )}
    </Formik>
  );
};

export default MigrationWithDowntimeQuestion;

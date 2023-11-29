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

import { Title, Space, Button, FormikInput, Group } from 'preflight/components/common';

type Props = {
  setShouldMigrateData: React.Dispatch<React.SetStateAction<boolean>>,
}

type FormValues = {
  host: string,
  username: string,
  password: string,
}

const DataMigration = ({ setShouldMigrateData }: Props) => {
  const onSubmit = (formValues: FormValues) => {
    console.log('===submitting', formValues);
  };

  const initialValues = {
    host: '',
    username: '',
    password: '',
  };

  return (
    <div>
      <Title order={3}>Migrate data from existing node</Title>
      <p>Migrate your your data from an existing OpenSearch node to a <em>Graylog data node</em>. </p>
      <Space h="md" />

      <Formik initialValues={initialValues} onSubmit={(formValues: FormValues) => onSubmit(formValues)}>
        {({ isSubmitting, isValid }) => (
          <Form>
            <FormikInput placeholder="Host"
                         name="host"
                         label="Node Host"
                         required />
            <FormikInput placeholder="Username"
                         name="username"
                         label="Username"
                         required />
            <FormikInput placeholder="Password"
                         name="password"
                         label="Password"
                         required />
            <Space h="md" />
            <Group>
              <Button disabled={isSubmitting || !isValid} type="submit">
                {isSubmitting ? 'Starting Migration' : 'Start Migration'}
              </Button>
              <Button onClick={() => setShouldMigrateData(false)} variant="light">Cancel</Button>
            </Group>
          </Form>
        )}
      </Formik>
    </div>
  );
};

export default DataMigration;

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
import styled from 'styled-components';
import { Space } from '@mantine/core';

import { Button, FormikInput } from 'preflight/components/common';
import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import UserNotification from 'preflight/util/UserNotification';

type FormValues = {

}

const StyledForm = styled(Form)`
  > div:not(:last-child) {
    margin-bottom: 10px;
  }
`;

const CACreateForm = () => {
  const onSubmit = (caData: FormValues) => fetch(
    'POST',
    qualifyUrl('/api/ca/create'),
    caData,
    false,
  ).then(() => {
    UserNotification.success('CA created successfully');
  }).catch((error) => {
    UserNotification.error(`CA creation failed with error: ${error}`);
  });

  return (
    <div>
      <p>
        Here you can quickly create a new certificate authority. It should only be used to secure your Graylog data nodes.
      </p>
      <Space h="sm" />
      <Formik initialValues={{ 'input-1': '', 'input-2': '' }} onSubmit={onSubmit}>
        {({ isSubmitting, isValid }) => (
          <StyledForm>
            <FormikInput placeholder="Input 1 placeholder"
                         name="input-1"
                         label="Input 1" />
            <FormikInput placeholder="Input 2 placeholder"
                         name="input-2"
                         label="Input 2" />
            <Button disabled={isSubmitting || !isValid} type="submit">
              {isSubmitting ? 'Creating CA...' : 'Create CA'}
            </Button>
          </StyledForm>
        )}
      </Formik>
    </div>
  );
};

export default CACreateForm;

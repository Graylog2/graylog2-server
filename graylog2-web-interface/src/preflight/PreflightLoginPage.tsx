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
import { Button, FormikInput, Space } from 'preflight/components/common';
import { Formik, Form } from 'formik';

import LoginChrome from 'components/login/LoginChrome';

const PreflightLoginPage = () => {
  console.log('test');

  return (
    <LoginChrome>
      <Formik initialValues={{ username: '', password: '' }} onSubmit={(formValues) => console.log(formValues)}>
        <Form>
          <FormikInput placeholder="Username"
                       label="Username"
                       name="username"
                       type="text"
                       required />
          <FormikInput placeholder="Password"
                       label="Password"
                       name="password"
                       type="password"
                       required />
          <Space h="md" />
          <Button type="submit">Sign in</Button>
        </Form>
      </Formik>
    </LoginChrome>
  );
};

export default PreflightLoginPage;

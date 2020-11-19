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
import { Formik, Form } from 'formik';
import * as React from 'react';
import { render, fireEvent, waitFor } from 'wrappedTestingLibrary';

import FormikInput from './FormikInput';

type FormValues = { [key: string]: unknown };

describe('<FormikInput />', () => {
  const SimpleForm = ({ children, onSubmit, initialValues }: { children: React.ReactElement, onSubmit: (FormValues) => Promise<void>, initialValues?: FormValues}) => (
    <Formik onSubmit={(data) => onSubmit(data)} initialValues={initialValues}>
      <Form>
        {children}
        <button type="submit">Submit Form</button>
      </Form>
    </Formik>
  );

  SimpleForm.defaultProps = {
    initialValues: {},
  };

  it('should update and submit correct value', async () => {
    const submitStub = jest.fn();
    const { getByLabelText, getByText } = render(
      <SimpleForm onSubmit={submitStub}>
        <FormikInput label="Username"
                     id="username"
                     name="username"
                     type="text" />
      </SimpleForm>,
    );

    const usernameInput = getByLabelText('Username');
    const submitButton = getByText('Submit Form');
    fireEvent.change(usernameInput, { target: { value: 'A username' } });
    fireEvent.click(submitButton);

    await waitFor(() => expect(submitStub).toHaveBeenCalledWith({ username: 'A username' }));
  });

  it('should submit correct initial value', async () => {
    const submitStub = jest.fn();
    const { getByText } = render(
      <SimpleForm onSubmit={submitStub} initialValues={{ username: 'Initial username' }}>
        <FormikInput label="Username"
                     id="username"
                     name="username"
                     type="text" />
      </SimpleForm>,
    );

    const submitButton = getByText('Submit Form');
    fireEvent.click(submitButton);

    await waitFor(() => expect(submitStub).toHaveBeenCalledWith({ username: 'Initial username' }));
  });

  it('should update and submit correct value for one checkbox', async () => {
    const submitStub = jest.fn();
    const { getByLabelText, getByText } = render(
      <SimpleForm onSubmit={submitStub}>
        <FormikInput label="Newsletter Subscription"
                     id="newsletter"
                     name="newsletter"
                     type="checkbox" />
      </SimpleForm>,
    );

    const newsletterCheckbox = getByLabelText('Newsletter Subscription');
    const submitButton = getByText('Submit Form');
    fireEvent.click(newsletterCheckbox);
    fireEvent.click(submitButton);

    await waitFor(() => expect(submitStub).toHaveBeenCalledWith({ newsletter: true }));
  });
});

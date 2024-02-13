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
import React from 'react';
import { render, screen, act } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';
import { Form, Formik } from 'formik';

import NestedForm from './NestedForm';

describe('NestedForm', () => {
  let originalConsoleError;

  beforeAll(() => {
    // eslint-disable-next-line no-console
    originalConsoleError = console.error;

    // eslint-disable-next-line no-console
    console.error = (message: string) => {
      if (!JSON.stringify(message || '').includes('Warning: validateDOMNesting')) {
        originalConsoleError(message);
      }
    };
  });

  afterAll(() => {
    // eslint-disable-next-line no-console
    console.error = originalConsoleError;
  });

  it('should not submit parent form', async () => {
    const onSubmitParentForm = jest.fn();
    const onSubmit = jest.fn();

    render(
      <Formik onSubmit={onSubmitParentForm} initialValues={{}}>
        <Form>
          <Formik onSubmit={onSubmit} initialValues={{}}>
            <NestedForm>
              <button type="submit">Submit</button>
            </NestedForm>
          </Formik>
        </Form>
      </Formik>,
    );

    // eslint-disable-next-line testing-library/no-unnecessary-act
    await act(async () => {
      await userEvent.click(await screen.findByRole('button', { name: /submit/i }));
    });

    expect(onSubmit).toHaveBeenCalledTimes(1);
    expect(onSubmitParentForm).not.toHaveBeenCalled();
  });

  it('should not reset parent form', async () => {
    const onResetParentForm = jest.fn();
    const onReset = jest.fn();

    render(
      <Formik onSubmit={(() => {})} onReset={onResetParentForm} initialValues={{}}>
        <Form>
          <Formik onSubmit={() => {}} onReset={onReset} initialValues={{}}>
            <NestedForm>
              <input type="reset" />
            </NestedForm>
          </Formik>
        </Form>
      </Formik>,
    );

    userEvent.click(await screen.findByRole('button', { name: /reset/i }));

    expect(onReset).toHaveBeenCalledTimes(1);
    expect(onResetParentForm).not.toHaveBeenCalled();
  });
});

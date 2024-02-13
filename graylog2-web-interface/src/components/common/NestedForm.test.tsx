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
import { render, screen } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

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
      <form onSubmit={onSubmitParentForm}>
        <NestedForm onSubmit={onSubmit}>
          <button type="submit">Submit</button>
        </NestedForm>
      </form>);

    userEvent.click(await screen.findByRole('button', { name: /submit/i }));

    expect(onSubmit).toHaveBeenCalledTimes(1);
    expect(onSubmitParentForm).not.toHaveBeenCalled();
  });
});

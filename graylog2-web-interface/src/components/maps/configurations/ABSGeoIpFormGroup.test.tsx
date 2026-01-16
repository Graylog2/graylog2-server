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
import { render, screen, waitFor } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';
import { Formik } from 'formik';

import type { GeoIpConfigType } from 'components/maps/configurations/types';

import ABSGeoIpFormGroup from './ABSGeoIpFormGroup';

describe('ABSGeoIpFormGroup', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  const defaultInitialValues = {
    container: '',
    account_name: '',
    key: { set_value: '' },
  } as Partial<GeoIpConfigType>;

  const existingConfig = {
    container: 'my-container',
    account_name: 'my-account',
    key: { is_set: true },
  } as Partial<GeoIpConfigType>;

  const SUT = (initialValues: Partial<GeoIpConfigType> = defaultInitialValues) => {
    const onSubmit = jest.fn();

    return render(
      <Formik initialValues={initialValues as GeoIpConfigType} onSubmit={onSubmit}>
        <ABSGeoIpFormGroup />
      </Formik>,
    );
  };

  it('should render all required form fields for new configuration', async () => {
    SUT();

    expect(await screen.findByLabelText(/azure blob container name/i)).toBeInTheDocument();
    expect(await screen.findByPlaceholderText(/your-account-name/i)).toBeInTheDocument();
    expect(screen.getByTestId('azure-account-key-input')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /reset password/i })).not.toBeInTheDocument();
  });

  it('should show reset password button for existing configuration', async () => {
    SUT(existingConfig);

    expect(await screen.findByRole('button', { name: /reset password/i })).toBeInTheDocument();
    expect(screen.queryByTestId('azure-account-key-input')).not.toBeInTheDocument();
  });

  it('should toggle between reset and undo password states', async () => {
    SUT(existingConfig);

    const resetButton = await screen.findByRole('button', { name: /reset password/i });
    userEvent.click(resetButton);

    await waitFor(() => {
      expect(screen.getByTestId('azure-account-key-input')).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /undo reset/i })).toBeInTheDocument();
    });

    const undoButton = screen.getByRole('button', { name: /undo reset/i });
    userEvent.click(undoButton);

    await waitFor(() => {
      expect(screen.queryByTestId('azure-account-key-input')).not.toBeInTheDocument();
      expect(screen.getByRole('button', { name: /reset password/i })).toBeInTheDocument();
    });
  });

  it('should allow input in all form fields', async () => {
    SUT();

    const containerInput = await screen.findByLabelText(/azure blob container name/i) as HTMLInputElement;
    const accountInput = await screen.findByPlaceholderText(/your-account-name/i) as HTMLInputElement;
    const keyInput = screen.getByTestId('azure-account-key-input') as HTMLInputElement;

    await userEvent.type(containerInput, 'test-container');
    await userEvent.type(accountInput, 'test-account');
    await userEvent.type(keyInput, 'secret-key');

    await waitFor(() => {
      expect(containerInput.value).toBe('test-container');
      expect(accountInput.value).toBe('test-account');
      expect(keyInput.value).toBe('secret-key');
    });
  });
});

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
  const newConfig = {
    azure_container: '',
    azure_account: '',
    azure_endpoint: '',
    azure_account_key: { set_value: '' },
  } as Partial<GeoIpConfigType>;

  const existingConfig = {
    azure_container: 'my-container',
    azure_account: 'my-account',
    azure_endpoint: 'https://myaccount.blob.core.windows.net',
    azure_account_key: { is_set: true },
  } as Partial<GeoIpConfigType>;

  const renderComponent = (initialValues: Partial<GeoIpConfigType> = newConfig) =>
    render(
      <Formik initialValues={initialValues as GeoIpConfigType} onSubmit={jest.fn()}>
        <ABSGeoIpFormGroup />
      </Formik>,
    );

  it('renders all form fields for new configuration', async () => {
    renderComponent();

    expect(await screen.findByLabelText(/azure blob container name/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/azure blob endpoint url/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/azure account name/i)).toBeInTheDocument();
    expect(screen.getByTestId('azure-account-key-input')).toBeInTheDocument();
  });

  it('shows reset password button for existing configuration', async () => {
    renderComponent(existingConfig);

    expect(await screen.findByRole('button', { name: /reset password/i })).toBeInTheDocument();
    expect(screen.queryByTestId('azure-account-key-input')).not.toBeInTheDocument();
  });

  it('toggles password reset state', async () => {
    renderComponent(existingConfig);

    // Click reset
    await userEvent.click(await screen.findByRole('button', { name: /reset password/i }));

    await waitFor(() => {
      expect(screen.getByTestId('azure-account-key-input')).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /undo reset/i })).toBeInTheDocument();
    });

    // Click undo
    await userEvent.click(screen.getByRole('button', { name: /undo reset/i }));

    await waitFor(() => {
      expect(screen.queryByTestId('azure-account-key-input')).not.toBeInTheDocument();
      expect(screen.getByRole('button', { name: /reset password/i })).toBeInTheDocument();
    });
  });

  it('updates formik values correctly', async () => {
    render(
      <Formik initialValues={existingConfig as GeoIpConfigType} onSubmit={jest.fn()}>
        {({ values }) => (
          <>
            <ABSGeoIpFormGroup />
            <div data-testid="key-value">{JSON.stringify(values.azure_account_key)}</div>
          </>
        )}
      </Formik>,
    );

    // Initial state: keep_value
    await waitFor(() => {
      expect(screen.getByTestId('key-value').textContent).toContain('keep_value');
    });

    // After reset: delete_value
    await userEvent.click(await screen.findByRole('button', { name: /reset password/i }));

    await waitFor(() => {
      expect(screen.getByTestId('key-value').textContent).toContain('delete_value');
    });

    // After undo: keep_value
    await userEvent.click(screen.getByRole('button', { name: /undo reset/i }));

    await waitFor(() => {
      expect(screen.getByTestId('key-value').textContent).toContain('keep_value');
    });
  });
});

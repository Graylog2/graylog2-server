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

import selectEvent from 'helpers/selectEvent';
import type { GeoIpConfigType } from 'components/maps/configurations/types';

import ABSGeoIpFormGroup from './ABSGeoIpFormGroup';

describe('ABSGeoIpFormGroup', () => {
  const newConfig = {
    azure_container: '',
    azure_account: '',
    azure_endpoint: '',
    azure_account_key: { set_value: '' },
    azure_auth_type: 'automatic',
  } as Partial<GeoIpConfigType>;

  const newKeySecretConfig = {
    azure_container: '',
    azure_account: '',
    azure_endpoint: '',
    azure_account_key: { set_value: '' },
    azure_auth_type: 'keysecret',
  } as Partial<GeoIpConfigType>;

  const existingKeySecretConfig = {
    azure_container: 'my-container',
    azure_account: 'my-account',
    azure_endpoint: 'https://myaccount.blob.core.windows.net',
    azure_account_key: { is_set: true },
    azure_auth_type: 'keysecret',
  } as Partial<GeoIpConfigType>;

  const renderComponent = (initialValues: Partial<GeoIpConfigType> = newConfig) =>
    render(
      <Formik initialValues={initialValues as GeoIpConfigType} onSubmit={jest.fn()}>
        <ABSGeoIpFormGroup />
      </Formik>,
    );

  describe('Authentication Type Selection', () => {
    it('renders authentication type selector', async () => {
      renderComponent();

      expect(await screen.findByText('Azure Authentication Type')).toBeInTheDocument();
      expect(screen.getByText('Automatic')).toBeInTheDocument();
    });

    it('defaults to automatic authentication type', async () => {
      renderComponent();

      expect(await screen.findByText('Automatic')).toBeInTheDocument();
      expect(screen.getByText(/Automatic authentication will attempt each of the following/)).toBeInTheDocument();
    });

    it('shows help text with environment variables for automatic auth', async () => {
      renderComponent();

      expect(await screen.findByText('Environment')).toBeInTheDocument();
      expect(screen.getAllByText('AZURE_CLIENT_ID').length).toBeGreaterThan(0);
      expect(screen.getByText('AZURE_CLIENT_SECRET')).toBeInTheDocument();
      expect(screen.getByText('Workload Identity')).toBeInTheDocument();
      expect(screen.getByText('Managed Identity')).toBeInTheDocument();
      expect(screen.getByText('Azure CLI')).toBeInTheDocument();
      expect(screen.getByText(/Broker/)).toBeInTheDocument();
      expect(screen.getByText(/Azure DefaultAzureCredential Documentation/)).toBeInTheDocument();
    });

    it('hides account name and key fields when automatic is selected', async () => {
      renderComponent();

      await waitFor(() => {
        expect(screen.queryByLabelText(/azure account name/i)).not.toBeInTheDocument();
        expect(screen.queryByTestId('azure-account-key-input')).not.toBeInTheDocument();
      });
    });

    it('shows account name and key fields when key & secret is selected', async () => {
      renderComponent(newKeySecretConfig);

      expect(await screen.findByLabelText(/azure account name/i)).toBeInTheDocument();
      expect(screen.getByTestId('azure-account-key-input')).toBeInTheDocument();
    });

    it('switches from automatic to key & secret', async () => {
      renderComponent();

      expect(await screen.findByText(/Automatic authentication will attempt each of the following/)).toBeInTheDocument();

      const authTypeSelect = await selectEvent.findSelectInput('Select Authentication Type');
      await selectEvent.select(authTypeSelect, 'Key & Secret');

      await waitFor(() => {
        expect(screen.queryByText(/Automatic authentication will attempt each of the following/)).not.toBeInTheDocument();
        expect(screen.getByLabelText(/azure account name/i)).toBeInTheDocument();
        expect(screen.getByTestId('azure-account-key-input')).toBeInTheDocument();
      });
    });
  });

  describe('Common Fields', () => {
    it('always shows container name and endpoint URL fields', async () => {
      renderComponent();

      expect(await screen.findByLabelText(/azure blob container name/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/azure blob endpoint url/i)).toBeInTheDocument();
    });
  });

  describe('Key & Secret Mode', () => {
    it('renders all form fields for new key/secret configuration', async () => {
      renderComponent(newKeySecretConfig);

      expect(await screen.findByLabelText(/azure blob container name/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/azure blob endpoint url/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/azure account name/i)).toBeInTheDocument();
      expect(screen.getByTestId('azure-account-key-input')).toBeInTheDocument();
    });

    it('shows reset password button for existing configuration', async () => {
      renderComponent(existingKeySecretConfig);

      expect(await screen.findByRole('button', { name: /reset password/i })).toBeInTheDocument();
      expect(screen.queryByTestId('azure-account-key-input')).not.toBeInTheDocument();
    });

    it('toggles password reset state', async () => {
      renderComponent(existingKeySecretConfig);

      await userEvent.click(await screen.findByRole('button', { name: /reset password/i }));

      await waitFor(() => {
        expect(screen.getByTestId('azure-account-key-input')).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /undo reset/i })).toBeInTheDocument();
      });

      await userEvent.click(screen.getByRole('button', { name: /undo reset/i }));

      await waitFor(() => {
        expect(screen.queryByTestId('azure-account-key-input')).not.toBeInTheDocument();
        expect(screen.getByRole('button', { name: /reset password/i })).toBeInTheDocument();
      });
    });

    it('updates formik values correctly for key reset', async () => {
      render(
        <Formik initialValues={existingKeySecretConfig as GeoIpConfigType} onSubmit={jest.fn()}>
          {({ values }) => (
            <>
              <ABSGeoIpFormGroup />
              <div data-testid="key-value">{JSON.stringify(values.azure_account_key)}</div>
            </>
          )}
        </Formik>,
      );

      await waitFor(() => {
        expect(screen.getByTestId('key-value').textContent).toContain('keep_value');
      });

      await userEvent.click(await screen.findByRole('button', { name: /reset password/i }));

      await waitFor(() => {
        expect(screen.getByTestId('key-value').textContent).toContain('delete_value');
      });


      await waitFor(() => {
        expect(screen.getByTestId('key-value').textContent).toContain('keep_value');
      });
    });
  });

  describe('Formik Integration', () => {
    it('updates azure_auth_type in formik when changing auth type', async () => {
      render(
        <Formik initialValues={newConfig as GeoIpConfigType} onSubmit={jest.fn()}>
          {({ values }) => (
            <>
              <ABSGeoIpFormGroup />
              <div data-testid="auth-type-value">{values.azure_auth_type}</div>
            </>
          )}
        </Formik>,
      );

      expect(await screen.findByTestId('auth-type-value')).toHaveTextContent('automatic');

      const authTypeSelect = await selectEvent.findSelectInput('Select Authentication Type');
      await selectEvent.select(authTypeSelect, 'Key & Secret');

      await waitFor(() => {
        expect(screen.getByTestId('auth-type-value')).toHaveTextContent('keysecret');
      });
    });
  });
});

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

import type { GeoIpConfigType } from 'components/maps/configurations/types';
import selectEvent from 'helpers/selectEvent';

import GeoIpResolverConfig from './GeoIpResolverConfig';

describe('GeoIpResolverConfig', () => {
  const defaultConfig: GeoIpConfigType = {
    enabled: false,
    enforce_graylog_schema: true,
    db_vendor_type: 'MAXMIND',
    city_db_path: '/etc/server/GeoLite2-City.mmdb',
    asn_db_path: '/etc/server/GeoLite2-ASN.mmdb',
    refresh_interval_unit: 'MINUTES',
    refresh_interval: 10,
    pull_from_cloud: undefined,
    gcs_project_id: undefined,
  };

  const mockUpdateConfig = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should display configuration summary and open modal on edit', async () => {
    render(<GeoIpResolverConfig config={{ ...defaultConfig, enabled: true }} updateConfig={mockUpdateConfig} />);

    expect(await screen.findByText(/geo-location processor configuration/i)).toBeInTheDocument();
    expect(screen.getByText('MaxMind GeoIP')).toBeInTheDocument();

    const editButton = screen.getByRole('button', { name: /edit configuration/i });
    userEvent.click(editButton);

    expect(
      await screen.findByRole('heading', { name: /update geo-location processor configuration/i }),
    ).toBeInTheDocument();
  });

  it('should submit configuration changes and close modal', async () => {
    mockUpdateConfig.mockResolvedValue({ ...defaultConfig, enabled: true });
    render(<GeoIpResolverConfig config={defaultConfig} updateConfig={mockUpdateConfig} />);

    userEvent.click(await screen.findByRole('button', { name: /edit configuration/i }));

    const enabledCheckbox = await screen.findByLabelText(/enable geo-location processor/i);
    userEvent.click(enabledCheckbox);

    const submitButton = screen.getByRole('button', { name: /update configuration/i });
    userEvent.click(submitButton);

    await waitFor(() => {
      expect(mockUpdateConfig).toHaveBeenCalledWith(expect.objectContaining({ enabled: true }));
      expect(
        screen.queryByRole('heading', { name: /update geo-location processor configuration/i }),
      ).not.toBeInTheDocument();
    });
  });

  it('should show cloud storage specific form groups based on selection', async () => {
    render(<GeoIpResolverConfig config={{ ...defaultConfig, enabled: true }} updateConfig={mockUpdateConfig} />);

    await userEvent.click(await screen.findByRole('button', { name: /edit configuration/i }));

    await selectEvent.chooseOption('select cloud storage', 'Google Cloud Storage');

    expect(
      screen.getByRole('textbox', {
        name: /googe cloud storage project id \(opt\.\)/i,
      }),
    ).toBeInTheDocument();

    await selectEvent.chooseOption('select cloud storage', 'Azure Blob Storage');

    expect(await screen.findByLabelText(/azure blob container name/i)).toBeInTheDocument();
  });
  it('should disable form fields when processor is disabled', async () => {
    render(<GeoIpResolverConfig config={defaultConfig} updateConfig={mockUpdateConfig} />);

    userEvent.click(await screen.findByRole('button', { name: /edit configuration/i }));

    await waitFor(() => {
      expect(screen.getByLabelText(/path to the city database/i)).toBeDisabled();
    });

    const enabledCheckbox = screen.getByLabelText(/enable geo-location processor/i);
    userEvent.click(enabledCheckbox);

    await waitFor(() => {
      expect(screen.getByLabelText(/path to the city database/i)).not.toBeDisabled();
    });
  });
});

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
import { renderPreflight, screen, waitFor } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';
import DefaultQueryClientProvider from 'DefaultQueryClientProvider';

import fetch from 'logic/rest/FetchProvider';
import UserNotification from 'preflight/util/UserNotification';
import { asMock } from 'helpers/mocking';
import useDataNodes from 'preflight/hooks/useDataNodes';
import { dataNodes } from 'fixtures/dataNodes';

import CertificateProvisioning from './CertificateProvisioning';

jest.mock('preflight/util/UserNotification', () => ({
  error: jest.fn(),
  success: jest.fn(),
}));

jest.mock('logic/rest/FetchProvider', () => jest.fn());

jest.mock('preflight/hooks/useDataNodes');

const logger = {
  // eslint-disable-next-line no-console
  log: console.log,
  // eslint-disable-next-line no-console
  warn: console.warn,
  error: () => {},
};

describe('CertificateProvisioning', () => {
  beforeEach(() => {
    asMock(fetch).mockReturnValue(Promise.resolve());

    asMock(useDataNodes).mockReturnValue({
      data: dataNodes,
      isFetching: false,
      isInitialLoading: false,
      error: undefined,
    });
  });

  it('should provision certificates', async () => {
    renderPreflight(<CertificateProvisioning />);

    userEvent.click(await screen.findByRole('button', { name: /provision certificate and continue/i }));

    await waitFor(() => expect(fetch).toHaveBeenCalledWith(
      'POST',
      expect.stringContaining('/api/generate'),
      undefined,
      false,
    ));

    expect(UserNotification.success).toHaveBeenCalledWith('Certificate provisioning successful');

    await screen.findByRole('button', { name: /provision certificate and continue/i });
  });

  it('should show error when certificate provisioning failed', async () => {
    asMock(fetch).mockImplementationOnce(() => Promise.reject(new Error('Error')));

    renderPreflight(
      <DefaultQueryClientProvider options={{ logger }}>
        <CertificateProvisioning />
      </DefaultQueryClientProvider>,
    );

    userEvent.click(await screen.findByRole('button', { name: /provision certificate and continue/i }));

    await waitFor(() => expect(fetch).toHaveBeenCalledWith(
      'POST',
      expect.stringContaining('/api/generate'),
      undefined,
      false,
    ));

    expect(UserNotification.error).toHaveBeenCalledWith('Certificate provisioning failed with error: Error: Error');

    await screen.findByRole('button', { name: /provision certificate and continue/i });
  });

  it('should disable provisioning when there are no data nodes', async () => {
    asMock(useDataNodes).mockReturnValue({
      data: [],
      isFetching: false,
      isInitialLoading: false,
      error: undefined,
    });

    renderPreflight(<CertificateProvisioning />);

    await screen.findByText('At least one Graylog data node needs to run before the certificate can be provisioned.');

    expect(await screen.findByRole('button', { name: /provision certificate and continue/i })).toBeDisabled();
  });
});

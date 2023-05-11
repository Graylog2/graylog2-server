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
import { render, screen, waitFor } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import fetch from 'logic/rest/FetchProvider';
import UserNotification from 'preflight/util/UserNotification';
import { asMock } from 'helpers/mocking';

import CertificateProvisioning from './CertificateProvisioning';

jest.mock('preflight/util/UserNotification', () => ({
  error: jest.fn(),
  success: jest.fn(),
}));

jest.mock('logic/rest/FetchProvider', () => jest.fn(() => Promise.resolve()));

describe('CertificateProvisioning', () => {
  it('should provision CA', async () => {
    render(<CertificateProvisioning />);

    userEvent.click(await screen.findByRole('button', { name: /provision certificate and continue/i }));

    await waitFor(() => expect(fetch).toHaveBeenCalledWith(
      'POST',
      expect.stringContaining('/api/generate'),
      undefined,
      false,
    ));

    expect(UserNotification.success).toHaveBeenCalledWith('CA provisioned successfully');
  });

  it('should show error when CA provisioning failed', async () => {
    asMock(fetch).mockImplementationOnce(() => Promise.reject(new Error('Error')));
    render(<CertificateProvisioning />);

    userEvent.click(await screen.findByRole('button', { name: /provision certificate and continue/i }));

    await waitFor(() => expect(fetch).toHaveBeenCalledWith(
      'POST',
      expect.stringContaining('/api/generate'),
      undefined,
      false,
    ));

    expect(UserNotification.error).toHaveBeenCalledWith('CA provisioning failed with error: Error: Error');
  });
});

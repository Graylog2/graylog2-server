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
import userEvent from '@testing-library/user-event';
import React from 'react';
import { renderPreflight, screen, waitFor } from 'wrappedTestingLibrary';

import fetch from 'logic/rest/FetchProvider';
import { asMock } from 'helpers/mocking';
import UserNotification from 'preflight/util/UserNotification';

import CAUpload from './CAUpload';

jest.mock('logic/rest/FetchProvider', () => jest.fn());

jest.mock('preflight/util/UserNotification', () => ({
  error: jest.fn(),
  success: jest.fn(),
}));

describe('CAUpload', () => {
  beforeEach(() => {
    asMock(fetch).mockReturnValue(Promise.resolve());
  });

  const files = [
    new File(['fileBits'], 'fileName', { type: 'application/x-pem-file' }),
  ];

  const findDropZone = async () => {
    const dropzoneContainer = await screen.findByTestId('upload-dropzone');

    // eslint-disable-next-line testing-library/no-node-access
    return dropzoneContainer.querySelector('input');
  };

  it('should upload CA', async () => {
    renderPreflight(<CAUpload />);

    const dropzone = await findDropZone();
    userEvent.upload(dropzone, files);

    await waitFor(() => expect(fetch).toHaveBeenCalledWith(
      'POST',
      expect.stringContaining('/api/ca/upload'),
      { files },
      false,
    ));

    expect(UserNotification.success).toHaveBeenCalledWith('CA uploaded successfully');
  });

  it('should show error when CA upload fails', async () => {
    asMock(fetch).mockImplementation(() => Promise.reject(new Error('Error')));
    renderPreflight(<CAUpload />);

    const dropzone = await findDropZone();
    userEvent.upload(dropzone, files);

    await waitFor(() => expect(fetch).toHaveBeenCalledWith(
      'POST',
      expect.stringContaining('/api/ca/upload'),
      { files },
      false,
    ));

    expect(UserNotification.error).toHaveBeenCalledWith('CA upload failed with error: Error: Error');
  });
});

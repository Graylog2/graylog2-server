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
import { render, screen, waitFor } from 'wrappedTestingLibrary';
import DefaultQueryClientProvider from 'DefaultQueryClientProvider';

import { fetchMultiPartFormData } from 'logic/rest/FetchProvider';
import { asMock, StoreMock as MockStore } from 'helpers/mocking';
import UserNotification from 'util/UserNotification';

import CAUpload from './CAUpload';

jest.mock('logic/rest/FetchProvider', () => ({ fetchMultiPartFormData: jest.fn() }));
jest.mock('stores/sessions/SessionStore', () => ({ SessionStore: MockStore(['isLoggedIn', jest.fn()]) }));
jest.mock('stores/system/SystemStore', () => ({ SystemStore: MockStore() }));

jest.mock('util/UserNotification', () => ({
  error: jest.fn(),
  success: jest.fn(),
}));

const logger = {
  // eslint-disable-next-line no-console
  log: console.log,
  // eslint-disable-next-line no-console
  warn: console.warn,
  error: () => {},
};

describe('CAUpload', () => {
  beforeEach(() => {
    asMock(fetchMultiPartFormData).mockReturnValue(Promise.resolve());
  });

  const files = [
    new File(['fileBits'], 'fileName', { type: 'application/x-pem-file' }),
  ];

  const formData = () => {
    const f = new FormData();
    files.forEach((file) => f.append('files', file));

    return f;
  };

  const findDropZone = async () => {
    const dropzoneContainer = await screen.findByTestId('upload-dropzone');

    // eslint-disable-next-line testing-library/no-node-access
    return dropzoneContainer.querySelector('input');
  };

  it('should upload CA', async () => {
    render(<CAUpload />);

    const dropzone = await findDropZone();
    userEvent.upload(dropzone, files);
    userEvent.click(await screen.findByRole('button', { name: /Upload CA/i }));

    await waitFor(() => expect(fetchMultiPartFormData).toHaveBeenCalledWith(
      expect.stringContaining('/ca/upload'),
      formData(),
      false,
    ));

    expect(UserNotification.success).toHaveBeenCalledWith('CA uploaded successfully');
  });

  it('should show error when CA upload fails', async () => {
    asMock(fetchMultiPartFormData).mockRejectedValue(new Error('Something bad happened'));

    render(
      <DefaultQueryClientProvider options={{ logger }}>
        <CAUpload />
      </DefaultQueryClientProvider>,
    );

    const dropzone = await findDropZone();
    userEvent.upload(dropzone, files);

    userEvent.click(await screen.findByRole('button', { name: /Upload CA/i }));

    await waitFor(() => expect(fetchMultiPartFormData).toHaveBeenCalledWith(
      expect.stringContaining('/ca/upload'),
      formData(),
      false,
    ));

    expect(UserNotification.error).toHaveBeenCalledWith('CA upload failed with error: Error: Something bad happened');
  });
});

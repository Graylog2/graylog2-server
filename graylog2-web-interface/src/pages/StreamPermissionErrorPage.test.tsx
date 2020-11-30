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
import { render } from 'wrappedTestingLibrary';
import suppressConsole from 'helpers/suppressConsole';

import FetchError from 'logic/errors/FetchError';

import StreamPermissionErrorPage from './StreamPermissionErrorPage';

describe('StreamPermissionErrorPage', () => {
  it('displays fetch error', () => {
    const response = { status: 403, body: { message: 'The request error message', streams: ['stream-1-id', 'stream-2-id'], type: 'MissingStreamPermission' } };

    suppressConsole(async () => {
      const { getByText } = render(<StreamPermissionErrorPage error={new FetchError('The request error message', response)} />);

      expect(getByText('Missing Stream Permissions')).not.toBeNull();
      expect(getByText('You need permissions for streams with the id: stream-1-id, stream-2-id.')).not.toBeNull();
    });
  });
});

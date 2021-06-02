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
import mockComponent from 'helpers/mocking/MockComponent';

import FetchError from 'logic/errors/FetchError';

import UnauthorizedErrorPage from './UnauthorizedErrorPage';

jest.mock('components/layout/Footer', () => mockComponent('Footer'));

describe('UnauthorizedErrorPage', () => {
  it('displays fetch error', () => {
    suppressConsole(async () => {
      const response = { status: 403, body: { message: 'The request error message' } };
      const { getByText } = render(<UnauthorizedErrorPage error={new FetchError('The request error message', response.status, response)} />);

      expect(getByText('Missing Permissions')).not.toBeNull();
      expect(getByText(/The request error message/)).not.toBeNull();
    });
  });
});

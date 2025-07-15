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
import { render, screen } from 'wrappedTestingLibrary';

import mockComponent from 'helpers/mocking/MockComponent';

import DataAdapterCreate from './DataAdapterCreate';

jest.mock('components/lookup-tables', () => ({
  DataAdapterForm: mockComponent('DataAdapterFormMock'),
}));

jest.mock('graylog-web-plugin/plugin', () => ({
  PluginStore: {
    exports: () => [
      {
        type: 'someType',
        displayName: 'Some Mocked Data Adapter Type',
      },
    ],
  },
}));

describe('<DataAdapterCreate />', () => {
  it('should render with empty parameters', async () => {
    const callback = () => {};
    const types = {};
    render(<DataAdapterCreate saved={callback} types={types} />);
    await screen.findByText(/select data adapter type/i);
  });

  it('should render for types with defined frontend components', async () => {
    const callback = () => {};
    const types = {
      someType: {
        type: 'someType',
      },
    };
    render(<DataAdapterCreate saved={callback} types={types} />);
    await screen.findByText(/select data adapter type/i);
  });

  describe('with mocked console.error', () => {
    // eslint-disable-next-line no-console
    const consoleError = console.error;

    beforeAll(() => {
      // eslint-disable-next-line no-console
      console.error = jest.fn();
    });

    afterAll(() => {
      // eslint-disable-next-line no-console
      console.error = consoleError;
    });

    it('should render for types without defined frontend components', async () => {
      const callback = () => {};
      const types = {
        unknownType: {
          type: 'unknownType',
        },
      };
      render(<DataAdapterCreate saved={callback} types={types} />);
      await screen.findByText(/select data adapter type/i);

      // eslint-disable-next-line no-console
      expect(console.error).toHaveBeenCalledTimes(1);
    });
  });
});

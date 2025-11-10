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
    render(<DataAdapterCreate saved={callback} onCancel={() => {}} />);
    await screen.findByText(/select data adapter type/i);
  });
});

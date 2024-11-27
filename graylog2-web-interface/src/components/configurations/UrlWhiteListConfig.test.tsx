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

import MockStore from 'helpers/mocking/StoreMock';

import UrlWhiteListConfig from './UrlWhiteListConfig';

const mockConfig = {
  entries: [
    {
      id: 'f7033f1f-d50f-4323-96df-294ede41d951',
      value: 'http://localhost:8080/system1/',
      title: 'Test Item 1',
      type: 'regex',
    },
    {
      id: '636a2d40-c4c5-40b9-ab3a-48cf7978e9af',
      value: 'http://localhost:8080/system2/',
      title: 'Test Item 2',
      type: 'regex',
    },
    {
      id: 'f28fd891-5f2d-4128-9a94-e97c1ab07a1f',
      value: 'http://localhost:8080/system3/',
      title: 'Test Item 3',
      type: 'literal',
    },
  ],
  disabled: false,
};

jest.mock('stores/configurations/ConfigurationsStore', () => ({
  ConfigurationsStore: MockStore(['getInitialState', () => ({
    configuration: {
      'org.graylog2.system.urlwhitelist.UrlWhitelist': mockConfig,
    },
  })]),
  ConfigurationsActions: {
    list: jest.fn(() => Promise.resolve()),
  },
}));

describe('UrlWhiteListConfig', () => {
  describe('render the UrlWhiteListConfig component', () => {
    it('should display Url list table', async () => {
      render(<UrlWhiteListConfig />);
      await screen.findByText('Test Item 1');
      await screen.findByText('Test Item 2');
      await screen.findByText('Test Item 3');
    });
  });
});

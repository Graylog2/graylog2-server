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
import { render, screen } from 'wrappedTestingLibrary';

import asMock from 'helpers/mocking/AsMock';
import suppressConsole from 'helpers/suppressConsole';
import MockStore from 'helpers/mocking/StoreMock';
import { configuration as mockConfiguration } from 'fixtures/configurations';
import ConfigurationsPage from 'pages/ConfigurationsPage';
import usePluginEntities from 'views/logic/usePluginEntities';
import SidecarConfig from 'components/configurations/SidecarConfig';

jest.mock('views/logic/usePluginEntities');
jest.mock('components/configurations/SearchesConfig', () => () => <span>Search Configuration</span>);
jest.mock('components/configurations/MessageProcessorsConfig', () => () => <span>Message Processors Configuration</span>);
jest.mock('components/configurations/SidecarConfig');

jest.mock('stores/decorators/DecoratorsStore', () => ({
  DecoratorsStore: MockStore(),
}));

jest.mock('stores/configurations/ConfigurationsStore', () => ({
  ConfigurationsStore: MockStore(['getInitialState', () => ({
    configuration: mockConfiguration,
  })]),
  ConfigurationsActions: {
    listWhiteListConfig: jest.fn(() => Promise.resolve()),
    list: jest.fn(() => Promise.resolve()),
    listMessageProcessorsConfig: jest.fn(() => Promise.resolve()),
    listPermissionsConfig: jest.fn(() => Promise.resolve()),
  },
}));

jest.mock('stores/streams/StreamsStore', () => ({
  StreamsActions: { listStreams: () => Promise.resolve() },
}));

const ComponentThrowingError = () => {
  throw new Error('Boom!');
};

const ComponentWorkingFine = () => (
  <span>It is all good!</span>
);

describe('ConfigurationsPage', () => {
  afterEach(() => {
    jest.resetAllMocks();
  });

  it('wraps core configuration elements with error boundary', async () => {
    asMock(usePluginEntities).mockReturnValue([]);
    asMock(SidecarConfig).mockImplementation(ComponentThrowingError);

    await suppressConsole(async () => {
      render(<ConfigurationsPage />);

      return screen.findByText('Boom!');
    });

    await screen.findByText('Message Processors Configuration');
  });

  it('wraps plugin configuration elements with error boundary', async () => {
    asMock(usePluginEntities).mockReturnValue([
      { configType: 'foo', component: ComponentThrowingError },
      { configType: 'bar', component: ComponentWorkingFine },
    ]);

    await suppressConsole(() => {
      render(<ConfigurationsPage />);

      return screen.findByText('Boom!');
    });

    await screen.findByText('It is all good!');
  });
});

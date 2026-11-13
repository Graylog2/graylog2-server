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
import usePluginEntities from 'hooks/usePluginEntities';

import ReplaySearchSidebar from './ReplaySearchSidebar';

jest.mock('hooks/usePluginEntities');
jest.mock('components/events/ReplaySearchSidebar/GeneralEventSideBar', () => () => <span>General sidebar</span>);

const PluginSideBar = () => <span>Plugin sidebar</span>;
const PluginEventDefinitionSideBar = () => <span>Plugin event definition sidebar</span>;

const mockPlugin = (useCondition?: () => boolean) =>
  asMock(usePluginEntities).mockReturnValue([
    {
      key: 'plugin-sidebar',
      component: PluginSideBar,
      eventDefinitionComponent: PluginEventDefinitionSideBar,
      useCondition,
    },
  ]);

describe('ReplaySearchSidebar', () => {
  it('renders the general sidebar when no plugin is registered', async () => {
    asMock(usePluginEntities).mockReturnValue([]);

    render(<ReplaySearchSidebar alertId="event-id" />);

    await screen.findByText('General sidebar');
  });

  it('renders the plugin sidebar when its condition is met', async () => {
    mockPlugin(() => true);

    render(<ReplaySearchSidebar alertId="event-id" />);

    await screen.findByText('Plugin sidebar');
  });

  it('renders the plugin sidebar when it has no condition', async () => {
    mockPlugin();

    render(<ReplaySearchSidebar alertId="event-id" />);

    await screen.findByText('Plugin sidebar');
  });

  it('renders the general sidebar when the plugin condition is not met', async () => {
    mockPlugin(() => false);

    render(<ReplaySearchSidebar alertId="event-id" />);

    await screen.findByText('General sidebar');
  });

  it('renders the plugin event definition sidebar for an event definition', async () => {
    mockPlugin(() => true);

    render(<ReplaySearchSidebar alertId={undefined} definitionId="definition-id" />);

    await screen.findByText('Plugin event definition sidebar');
  });

  it('falls back to the general sidebar when the plugin condition changes', async () => {
    const useCondition = jest.fn(() => true);
    mockPlugin(useCondition);

    const { rerender } = render(<ReplaySearchSidebar alertId="event-id" />);

    await screen.findByText('Plugin sidebar');

    useCondition.mockReturnValue(false);
    rerender(<ReplaySearchSidebar alertId="event-id" />);

    await screen.findByText('General sidebar');
  });
});

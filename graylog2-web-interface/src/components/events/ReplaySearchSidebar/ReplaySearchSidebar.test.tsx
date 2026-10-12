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
import type { EventDefinitionSideBarDetailsProps, EventReplaySideBarDetailsProps } from 'views/types';

import ReplaySearchSidebar from './ReplaySearchSidebar';

jest.mock('hooks/usePluginEntities');

jest.mock('components/events/ReplaySearchSidebar/GeneralEventSideBar', () => ({
  __esModule: true,
  default: ({ alertId, definitionId }: EventReplaySideBarDetailsProps) => (
    <div data-testid="general-sidebar">
      general:{alertId}:{definitionId ?? ''}
    </div>
  ),
}));

const PluginComponent = ({ alertId, definitionId }: EventReplaySideBarDetailsProps) => (
  <div data-testid="plugin-sidebar">
    plugin:{alertId}:{definitionId ?? ''}
  </div>
);

const PluginDefinitionComponent = ({ definitionId }: EventDefinitionSideBarDetailsProps) => (
  <div data-testid="plugin-definition-sidebar">definition:{definitionId}</div>
);

const mockSideBarDetails = (plugin: unknown) => {
  asMock(usePluginEntities).mockImplementation(
    (entityKey) =>
      ({
        'views.components.eventReplay.sideBarDetails': plugin ? [plugin] : [],
      })[entityKey as string],
  );
};

describe('ReplaySearchSidebar', () => {
  beforeEach(() => {
    asMock(usePluginEntities).mockReturnValue([]);
  });

  it('renders GeneralEventSideBar when no plugin is registered', () => {
    render(<ReplaySearchSidebar alertId="alert-1" definitionId="def-1" />);

    expect(screen.getByTestId('general-sidebar')).toHaveTextContent('general:alert-1:def-1');
    expect(screen.queryByTestId('plugin-sidebar')).not.toBeInTheDocument();
  });

  it('renders the plugin component when a plugin is registered', () => {
    mockSideBarDetails({ key: 'test', component: PluginComponent });

    render(<ReplaySearchSidebar alertId="alert-1" definitionId="def-1" />);

    expect(screen.getByTestId('plugin-sidebar')).toHaveTextContent('plugin:alert-1:def-1');
    expect(screen.queryByTestId('general-sidebar')).not.toBeInTheDocument();
  });

  it('falls back to GeneralEventSideBar when useCondition returns false', () => {
    mockSideBarDetails({
      key: 'test',
      component: PluginComponent,
      useCondition: () => false,
    });

    render(<ReplaySearchSidebar alertId="alert-1" />);

    expect(screen.getByTestId('general-sidebar')).toHaveTextContent('general:alert-1:');
    expect(screen.queryByTestId('plugin-sidebar')).not.toBeInTheDocument();
  });

  it('uses the plugin component when useCondition returns true', () => {
    mockSideBarDetails({
      key: 'test',
      component: PluginComponent,
      useCondition: () => true,
    });

    render(<ReplaySearchSidebar alertId="alert-1" />);

    expect(screen.getByTestId('plugin-sidebar')).toBeInTheDocument();
  });

  it('renders eventDefinitionComponent when called without an alertId', () => {
    mockSideBarDetails({
      key: 'test',
      component: PluginComponent,
      eventDefinitionComponent: PluginDefinitionComponent,
    });

    render(<ReplaySearchSidebar alertId="" definitionId="def-1" />);

    expect(screen.getByTestId('plugin-definition-sidebar')).toHaveTextContent('definition:def-1');
    expect(screen.queryByTestId('plugin-sidebar')).not.toBeInTheDocument();
  });

  it('falls back to the plugin component when no eventDefinitionComponent is registered', () => {
    mockSideBarDetails({ key: 'test', component: PluginComponent });

    render(<ReplaySearchSidebar alertId="" definitionId="def-1" />);

    expect(screen.getByTestId('plugin-sidebar')).toHaveTextContent('plugin::def-1');
    expect(screen.queryByTestId('plugin-definition-sidebar')).not.toBeInTheDocument();
  });

  it('uses the plugin component (not the eventDefinitionComponent) when alertId is present', () => {
    mockSideBarDetails({
      key: 'test',
      component: PluginComponent,
      eventDefinitionComponent: PluginDefinitionComponent,
    });

    render(<ReplaySearchSidebar alertId="alert-1" definitionId="def-1" />);

    expect(screen.getByTestId('plugin-sidebar')).toHaveTextContent('plugin:alert-1:def-1');
    expect(screen.queryByTestId('plugin-definition-sidebar')).not.toBeInTheDocument();
  });
});

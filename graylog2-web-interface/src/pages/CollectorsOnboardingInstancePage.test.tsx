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
import { useInstance } from 'components/collectors/hooks/useInstanceQueries';
import { useFleet } from 'components/collectors/hooks/useFleetQueries';
import type { CollectorInstanceView } from 'components/collectors/types';

import CollectorsOnboardingInstancePage from './CollectorsOnboardingInstancePage';

jest.mock('components/collectors/hooks/useInstanceQueries', () => ({
  useInstance: jest.fn(),
}));

jest.mock('components/collectors/hooks/useFleetQueries', () => ({
  useFleet: jest.fn(),
}));

// ConnectionSuccess pulls in live search hooks — stub it.
jest.mock(
  'components/collectors/overview/onboarding/ConnectionSuccess',
  () =>
    function ConnectionSuccessStub({
      instance,
      fleetName = undefined,
      platformId = undefined,
    }: {
      instance: { hostname: string | null };
      fleetName?: string;
      platformId?: string;
    }) {
      return (
        <div>
          <span>Collector connected</span>
          <span>{instance.hostname}</span>
          <span>{fleetName}</span>
          <span data-testid="platform-id">{platformId ?? 'none'}</span>
        </div>
      );
    },
);

const mockUseParams = jest.fn();
const mockUseLocation = jest.fn();

jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useParams: () => mockUseParams(),
}));

jest.mock('routing/useLocation', () => ({
  __esModule: true,
  default: () => mockUseLocation(),
}));

const instance = {
  id: 'uid-42',
  instance_uid: 'uid-42',
  fleet_id: 'fleet-1',
  status: 'online',
  hostname: 'web-prod-01',
  version: '1.2.3',
} as CollectorInstanceView;

describe('CollectorsOnboardingInstancePage', () => {
  beforeEach(() => {
    jest.clearAllMocks();

    mockUseParams.mockReturnValue({ instanceUid: 'uid-42' });
    mockUseLocation.mockReturnValue({ state: null });
    asMock(useInstance).mockReturnValue({ data: instance, isLoading: false, error: null } as ReturnType<
      typeof useInstance
    >);
    asMock(useFleet).mockReturnValue({ data: { id: 'fleet-1', name: 'Default Fleet' } } as ReturnType<typeof useFleet>);
  });

  it('renders the connection result for the instance', () => {
    render(<CollectorsOnboardingInstancePage />);

    expect(useInstance).toHaveBeenCalledWith('uid-42');
    expect(screen.getByText('Collector connected')).toBeInTheDocument();
    expect(screen.getByText('web-prod-01')).toBeInTheDocument();
    expect(screen.getByText('Default Fleet')).toBeInTheDocument();
  });

  it('passes the platform from router location state', () => {
    mockUseLocation.mockReturnValue({ state: { platformId: 'linux' } });

    render(<CollectorsOnboardingInstancePage />);

    expect(screen.getByTestId('platform-id')).toHaveTextContent('linux');
  });

  it('shows a spinner while loading', () => {
    asMock(useInstance).mockReturnValue({ data: undefined, isLoading: true, error: null } as ReturnType<
      typeof useInstance
    >);

    render(<CollectorsOnboardingInstancePage />);

    expect(screen.queryByText('Collector connected')).not.toBeInTheDocument();
  });

  it('shows a not-found message with a link to instances for an unknown uid', () => {
    asMock(useInstance).mockReturnValue({ data: null, isLoading: false, error: null } as ReturnType<
      typeof useInstance
    >);

    render(<CollectorsOnboardingInstancePage />);

    expect(screen.getByText(/collector instance not found/i)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /instances/i })).toHaveAttribute(
      'href',
      expect.stringContaining('/system/collectors/instances'),
    );
  });

  it('surfaces a fetch error', () => {
    asMock(useInstance).mockReturnValue({
      data: undefined,
      isLoading: false,
      error: new Error('boom'),
    } as ReturnType<typeof useInstance>);

    render(<CollectorsOnboardingInstancePage />);

    expect(screen.getByText(/could not load collector instance/i)).toBeInTheDocument();
  });
});

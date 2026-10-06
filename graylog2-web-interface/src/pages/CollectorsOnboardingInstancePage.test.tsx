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
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import useSendCollectorsTelemetry from 'components/collectors/hooks/useSendCollectorsTelemetry';
import { useInstance } from 'components/collectors/hooks/useInstanceQueries';
import { useFleet } from 'components/collectors/hooks/useFleetQueries';
import useFinishOnboarding from 'components/welcome/hooks/useFinishOnboarding';
import useOnboardingEligibility from 'components/welcome/hooks/useOnboardingEligibility';
import type { CollectorInstanceView } from 'components/collectors/types';

import CollectorsOnboardingInstancePage from './CollectorsOnboardingInstancePage';

jest.mock('components/collectors/hooks/useSendCollectorsTelemetry');
jest.mock('components/welcome/hooks/useFinishOnboarding');
jest.mock('components/welcome/hooks/useOnboardingEligibility');

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
    }: {
      instance: { hostname: string | null };
      fleetName?: string;
    }) {
      return (
        <div>
          <span>Collector connected</span>
          <span>{instance.hostname}</span>
          <span>{fleetName}</span>
        </div>
      );
    },
);

const mockUseLocation = jest.fn();

jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useParams: () => ({ instanceUid: 'uid-42' }),
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
  const mockInstanceLookup = (
    overrides: { data?: CollectorInstanceView | null; isLoading?: boolean; error?: Error | null } = {},
  ) =>
    asMock(useInstance).mockReturnValue({
      data: instance,
      isLoading: false,
      error: null,
      ...overrides,
    } as ReturnType<typeof useInstance>);

  const sendTelemetry = jest.fn();
  const finish = jest.fn();

  const mockOnboardingStatus = (status: 'setup' | 'finished' | 'dismissed' | 'unknown') =>
    asMock(useOnboardingEligibility).mockReturnValue({ data: { status }, isLoading: false } as ReturnType<
      typeof useOnboardingEligibility
    >);

  beforeEach(() => {
    jest.clearAllMocks();

    asMock(useSendCollectorsTelemetry).mockReturnValue(sendTelemetry);
    asMock(useFinishOnboarding).mockReturnValue({ mutate: finish } as unknown as ReturnType<
      typeof useFinishOnboarding
    >);
    mockUseLocation.mockReturnValue({ state: null });
    mockOnboardingStatus('setup');
    mockInstanceLookup();
    asMock(useFleet).mockReturnValue({ data: { id: 'fleet-1', name: 'Default Fleet' } } as ReturnType<typeof useFleet>);
  });

  it('renders the connection result for the instance', () => {
    render(<CollectorsOnboardingInstancePage />);

    expect(useInstance).toHaveBeenCalledWith('uid-42');
    expect(screen.getByText('Collector connected')).toBeInTheDocument();
    expect(screen.getByText('web-prod-01')).toBeInTheDocument();
    expect(screen.getByText('Default Fleet')).toBeInTheDocument();
  });

  it('falls back to the fleet name from location state while the fleet loads', () => {
    mockUseLocation.mockReturnValue({ state: { fleetName: 'Fresh Fleet' } });
    asMock(useFleet).mockReturnValue({ data: undefined } as ReturnType<typeof useFleet>);

    render(<CollectorsOnboardingInstancePage />);

    expect(screen.getByText('Fresh Fleet')).toBeInTheDocument();
  });

  it('shows a spinner while loading', () => {
    mockInstanceLookup({ data: undefined, isLoading: true });

    render(<CollectorsOnboardingInstancePage />);

    expect(screen.queryByText('Collector connected')).not.toBeInTheDocument();
  });

  it('shows a not-found message with a link to instances for an unknown uid', () => {
    mockInstanceLookup({ data: null });

    render(<CollectorsOnboardingInstancePage />);

    expect(screen.getByText(/collector instance not found/i)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /instances/i })).toHaveAttribute(
      'href',
      expect.stringContaining('/system/collectors/instances'),
    );
  });

  it('finishes the onboarding once the instance is loaded', () => {
    render(<CollectorsOnboardingInstancePage />);

    expect(finish).toHaveBeenCalledTimes(1);
  });

  it('does not finish the onboarding while the instance is still loading', () => {
    mockInstanceLookup({ data: undefined, isLoading: true });

    render(<CollectorsOnboardingInstancePage />);

    expect(finish).not.toHaveBeenCalled();
  });

  // `/onboarding/finish` requires `clusterconfigentry:edit`, which collector roles do not hold.
  // `GET /onboarding` degrades to `unknown` instead of 403 for those users, so treating anything
  // but `setup` as "not our onboarding to finish" keeps them off the forbidden endpoint.
  it.each(['finished', 'dismissed', 'unknown'] as const)(
    'does not finish the onboarding when its status is %s',
    (status) => {
      mockOnboardingStatus(status);

      render(<CollectorsOnboardingInstancePage />);

      expect(finish).not.toHaveBeenCalled();
    },
  );

  it('does not finish the onboarding while its status is still loading', () => {
    asMock(useOnboardingEligibility).mockReturnValue({ data: undefined, isLoading: true } as ReturnType<
      typeof useOnboardingEligibility
    >);

    render(<CollectorsOnboardingInstancePage />);

    expect(finish).not.toHaveBeenCalled();
  });

  // `useInstance` polls on the heartbeat interval and returns a fresh object each time, so a
  // re-render with an equal-but-not-identical instance must not re-POST the completion.
  it('does not finish the onboarding again when the polled instance object changes identity', () => {
    const { rerender } = render(<CollectorsOnboardingInstancePage />);

    expect(finish).toHaveBeenCalledTimes(1);

    mockInstanceLookup({ data: { ...instance } });
    rerender(<CollectorsOnboardingInstancePage />);
    mockInstanceLookup({ data: { ...instance } });
    rerender(<CollectorsOnboardingInstancePage />);

    expect(finish).toHaveBeenCalledTimes(1);
  });

  it('no longer emits onboarding telemetry from the page itself', () => {
    render(<CollectorsOnboardingInstancePage />);

    expect(sendTelemetry).not.toHaveBeenCalledWith(
      TELEMETRY_EVENT_TYPE.COLLECTORS.ONBOARDING.COMPLETED,
      expect.anything(),
    );
  });

  it('surfaces a fetch error', () => {
    mockInstanceLookup({ data: undefined, error: new Error('boom') });

    render(<CollectorsOnboardingInstancePage />);

    expect(screen.getByText(/could not load collector instance/i)).toBeInTheDocument();
  });
});

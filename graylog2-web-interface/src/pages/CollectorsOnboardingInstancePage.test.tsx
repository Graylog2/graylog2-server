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
import { render, screen, waitFor } from 'wrappedTestingLibrary';
import { type Location } from 'react-router-dom';

import asMock from 'helpers/mocking/AsMock';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { useInstance } from 'components/collectors/hooks/useInstanceQueries';
import { useFleet } from 'components/collectors/hooks/useFleetQueries';
import type { CollectorInstanceView } from 'components/collectors/types';
import collectorReceivedMessagesUrl from 'components/collectors/common/collectorReceivedMessagesUrl';
import { COLLECTOR_INSTANCE_UID_FIELD } from 'components/collectors/common/fields';
import useDefaultInterval from 'views/hooks/useDefaultIntervalForRefresh';
import mockHistory from 'helpers/mocking/mockHistory';
import useHistory from 'routing/useHistory';
import useLocation from 'routing/useLocation';

import CollectorsOnboardingInstancePage from './CollectorsOnboardingInstancePage';

jest.mock('logic/telemetry/useSendTelemetry');

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

jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useParams: () => ({ instanceUid: 'uid-42' }),
}));

jest.mock('views/hooks/useDefaultIntervalForRefresh');

jest.mock('routing/useHistory');

jest.mock('routing/useLocation');

const instance = {
  id: 'uid-42',
  instance_uid: 'uid-42',
  fleet_id: 'fleet-1',
  status: 'online',
  hostname: 'web-prod-01',
  version: '1.2.3',
} as CollectorInstanceView;

describe('CollectorsOnboardingInstancePage', () => {
  const history = mockHistory();

  const mockInstanceLookup = (
    overrides: { data?: CollectorInstanceView | null; isLoading?: boolean; error?: Error | null } = {},
  ) =>
    asMock(useInstance).mockReturnValue({
      data: instance,
      isLoading: false,
      error: null,
      isError: false,
      ...overrides,
    });

  beforeEach(() => {
    jest.clearAllMocks();

    asMock(useSendTelemetry).mockReturnValue(jest.fn());
    asMock(useHistory).mockReturnValue(history);
    asMock(useLocation).mockReturnValue({ state: null } as Location);
    mockInstanceLookup();
    asMock(useFleet).mockReturnValue({ data: { id: 'fleet-1', name: 'Default Fleet' } } as ReturnType<typeof useFleet>);
    asMock(useDefaultInterval).mockReturnValue(null);
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

  it('navigates to the search page once the instance and default interval are both available', async () => {
    asMock(useDefaultInterval).mockReturnValue('PT5S');

    render(<CollectorsOnboardingInstancePage />);

    await waitFor(() => {
      expect(history.push).toHaveBeenCalledWith(
        collectorReceivedMessagesUrl(COLLECTOR_INSTANCE_UID_FIELD, instance.instance_uid, 'PT5S'),
      );
    });
  });

  it('does not navigate before the default interval has loaded', () => {
    asMock(useDefaultInterval).mockReturnValue(null);

    render(<CollectorsOnboardingInstancePage />);

    expect(history.push).not.toHaveBeenCalled();
  });

  it('surfaces a fetch error', () => {
    mockInstanceLookup({ data: undefined, error: new Error('boom') });

    render(<CollectorsOnboardingInstancePage />);

    expect(screen.getByText(/could not load collector instance/i)).toBeInTheDocument();
  });
});

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
import { renderHook } from 'wrappedTestingLibrary/hooks';
import { MemoryRouter } from 'react-router-dom';
import * as React from 'react';

import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { asMock } from 'helpers/mocking';

import useSendCollectorsTelemetry from './useSendCollectorsTelemetry';

jest.mock('logic/telemetry/useSendTelemetry');

const wrapper =
  (pathname: string) =>
  ({ children }: { children: React.ReactNode }) => <MemoryRouter initialEntries={[pathname]}>{children}</MemoryRouter>;

describe('useSendCollectorsTelemetry', () => {
  const sendTelemetry = jest.fn();

  beforeEach(() => {
    asMock(useSendTelemetry).mockReturnValue(sendTelemetry);
    sendTelemetry.mockClear();
  });

  it.each([
    ['/system/collectors', 'collectors-overview'],
    ['/system/collectors/fleets', 'collectors-fleets'],
    ['/system/collectors/fleets/fleet-1', 'collectors-fleet-detail'],
    ['/system/collectors/fleets/fleet-1?tab=sources', 'collectors-fleet-detail'],
    ['/system/collectors/instances', 'collectors-instances'],
    ['/system/collectors/deployment', 'collectors-deployment'],
    ['/system/collectors/settings', 'collectors-settings'],
  ])('derives app_section %s for pathname %s', (pathname, expected) => {
    const { result } = renderHook(() => useSendCollectorsTelemetry(), { wrapper: wrapper(pathname) });

    result.current('Fleet Created' as never, { app_action_value: 'x', fleet_id: 'f1' });

    expect(sendTelemetry).toHaveBeenCalledWith('Fleet Created', {
      app_section: expected,
      app_action_value: 'x',
      fleet_id: 'f1',
    });
  });

  it('allows the caller to override app_section explicitly', () => {
    const { result } = renderHook(() => useSendCollectorsTelemetry(), { wrapper: wrapper('/system/collectors') });

    result.current('Fleet Created' as never, { app_section: 'custom-section', fleet_id: 'f1' });

    expect(sendTelemetry).toHaveBeenCalledWith(
      'Fleet Created',
      expect.objectContaining({ app_section: 'custom-section' }),
    );
  });
});

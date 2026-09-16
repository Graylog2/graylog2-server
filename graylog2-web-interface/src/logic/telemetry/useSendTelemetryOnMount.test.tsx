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

import useSendTelemetryOnMount from './useSendTelemetryOnMount';

describe('useSendTelemetryOnMount', () => {
  it('sends the event once on mount', () => {
    const sendTelemetry = jest.fn();

    const { rerender } = renderHook(() =>
      useSendTelemetryOnMount(sendTelemetry, '$pageview', { app_action_value: 'opened' }),
    );

    expect(sendTelemetry).toHaveBeenCalledTimes(1);
    expect(sendTelemetry).toHaveBeenCalledWith('$pageview', { app_action_value: 'opened' });

    rerender();

    expect(sendTelemetry).toHaveBeenCalledTimes(1);
  });

  it('resends the event when an extra dependency changes', () => {
    const sendTelemetry = jest.fn();

    const { rerender } = renderHook(
      ({ dep }) => useSendTelemetryOnMount(sendTelemetry, '$pageview', { app_action_value: 'opened' }, [dep]),
      { initialProps: { dep: 'a' } },
    );

    expect(sendTelemetry).toHaveBeenCalledTimes(1);

    rerender({ dep: 'a' });

    expect(sendTelemetry).toHaveBeenCalledTimes(1);

    rerender({ dep: 'b' });

    expect(sendTelemetry).toHaveBeenCalledTimes(2);
  });
});

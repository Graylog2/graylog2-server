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
import { render } from 'wrappedTestingLibrary';

import { asMock } from 'helpers/mocking';
import { createSearch } from 'fixtures/searches';
import View from 'views/logic/views/View';
import TestStoreProvider from 'views/test/TestStoreProvider';
import useViewsPlugin from 'views/test/testViewsPlugin';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import TelemetryContext from 'logic/telemetry/TelemetryContext';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

import IlluminateDashboardViewTelemetry from './IlluminateDashboardViewTelemetry';

jest.mock('logic/telemetry/useSendTelemetry');

const illuminateDashboard = createSearch().toBuilder().type(View.Type.Dashboard).scope('ILLUMINATE').build();

describe('IlluminateDashboardViewTelemetry', () => {
  const sendTelemetry = jest.fn();

  useViewsPlugin();

  const SUT = ({ view, isNew = false }: { view: View; isNew?: boolean }) => (
    <TestStoreProvider view={view} isNew={isNew}>
      <IlluminateDashboardViewTelemetry />
    </TestStoreProvider>
  );

  const telemetryContextValue = () => ({ sendTelemetry: jest.fn(), sendErrorReport: jest.fn() });

  beforeEach(() => {
    sendTelemetry.mockClear();
    asMock(useSendTelemetry).mockReturnValue(sendTelemetry);
  });

  it('sends view telemetry for an Illuminate dashboard', () => {
    render(<SUT view={illuminateDashboard} />);

    expect(sendTelemetry).toHaveBeenCalledWith(TELEMETRY_EVENT_TYPE.DASHBOARD_ACTION.ILLUMINATE_DASHBOARD_VIEWED, {
      app_action_value: 'illuminate-dashboard-view',
      event_details: { dashboard_title: 'Search 1' },
    });
  });

  it('does not send view telemetry for a non-Illuminate dashboard', () => {
    render(<SUT view={createSearch().toBuilder().type(View.Type.Dashboard).build()} />);

    expect(sendTelemetry).not.toHaveBeenCalled();
  });

  it('does not send view telemetry for an Illuminate search', () => {
    render(<SUT view={illuminateDashboard.toBuilder().type(View.Type.Search).build()} />);

    expect(sendTelemetry).not.toHaveBeenCalled();
  });

  it('does not send view telemetry for a new, unsaved Illuminate dashboard', () => {
    render(<SUT view={illuminateDashboard} isNew />);

    expect(sendTelemetry).not.toHaveBeenCalled();
  });

  it('does not send again when only `sendTelemetry` changes for the same view', () => {
    const { rerender } = render(<SUT view={illuminateDashboard} />);

    const nextSendTelemetry = jest.fn();
    asMock(useSendTelemetry).mockReturnValue(nextSendTelemetry);
    rerender(<SUT view={illuminateDashboard} />);

    expect(sendTelemetry).toHaveBeenCalledTimes(1);
    expect(nextSendTelemetry).not.toHaveBeenCalled();
  });

  it('sends again when the telemetry context changes, so events dropped before telemetry is ready are retried', () => {
    const { rerender } = render(
      <TelemetryContext.Provider value={telemetryContextValue()}>
        <SUT view={illuminateDashboard} />
      </TelemetryContext.Provider>,
    );

    rerender(
      <TelemetryContext.Provider value={telemetryContextValue()}>
        <SUT view={illuminateDashboard} />
      </TelemetryContext.Provider>,
    );

    expect(sendTelemetry).toHaveBeenCalledTimes(2);
  });
});

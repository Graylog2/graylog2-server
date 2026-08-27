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
import userEvent from '@testing-library/user-event';

import { asMock } from 'helpers/mocking';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import useSendCollectorsTelemetry from 'components/collectors/hooks/useSendCollectorsTelemetry';
import { useCollectorsConfig, useEnrollmentTokenCount } from 'components/collectors/hooks';

import CollectorsDeploymentPage from './CollectorsDeploymentPage';

jest.mock('components/collectors/hooks/useSendCollectorsTelemetry');

jest.mock('components/collectors/hooks', () => ({
  useCollectorsConfig: jest.fn(),
  useEnrollmentTokenCount: jest.fn(),
}));

jest.mock('components/collectors/deployment', () => ({
  DeployTab: () => <div>deploy tab content</div>,
  EnrollmentTokenList: () => <div>token list content</div>,
}));

describe('CollectorsDeploymentPage', () => {
  const sendTelemetry = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();

    asMock(useSendCollectorsTelemetry).mockReturnValue(sendTelemetry);
    asMock(useCollectorsConfig).mockReturnValue({
      data: { signing_cert_id: 'cert' },
      isLoading: false,
    } as ReturnType<typeof useCollectorsConfig>);
    asMock(useEnrollmentTokenCount).mockReturnValue(3);
  });

  it('reports switching between the Deploy and Enrollment tokens tabs', async () => {
    const user = userEvent.setup();
    render(<CollectorsDeploymentPage />);

    await user.click(screen.getByRole('tab', { name: /enrollment tokens/i }));

    expect(sendTelemetry).toHaveBeenCalledWith(TELEMETRY_EVENT_TYPE.COLLECTORS.DEPLOYMENT.TAB_SELECTED, {
      app_action_value: 'tab-tokens',
      tab: 'tokens',
    });

    await user.click(screen.getByRole('tab', { name: /deploy/i }));

    expect(sendTelemetry).toHaveBeenCalledWith(TELEMETRY_EVENT_TYPE.COLLECTORS.DEPLOYMENT.TAB_SELECTED, {
      app_action_value: 'tab-deploy',
      tab: 'deploy',
    });
  });
});

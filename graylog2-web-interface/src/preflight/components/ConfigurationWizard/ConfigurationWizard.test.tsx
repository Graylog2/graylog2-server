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
import { renderPreflight, screen } from 'wrappedTestingLibrary';

import ConfigurationWizard from 'preflight/components/ConfigurationWizard';
import { CONFIGURATION_STEPS } from 'preflight/Constants';
import useConfigurationStep from 'preflight/hooks/useConfigurationStep';
import { asMock } from 'helpers/mocking';

jest.mock('preflight/hooks/useConfigurationStep');

jest.mock('preflight/hooks/useDataNodes', () => jest.fn(() => ({
  data: [],
  isFetching: false,
  isInitialLoading: false,
  error: undefined,
})));

jest.mock('preflight/hooks/useDataNodesCA', () => jest.fn(() => ({
  data: undefined,
  isInitialLoading: false,
  isFetching: false,
  error: undefined,
})));

describe('ConfigurationWizard', () => {
  it('should show CA configuration step', async () => {
    asMock(useConfigurationStep).mockReturnValue({ step: CONFIGURATION_STEPS.CA_CONFIGURATION.key, isLoading: false, errors: null });
    renderPreflight(<ConfigurationWizard setIsWaitingForStartup={() => {}} />);

    await screen.findByText(/In this first step you can either upload or create a new certificate authority./);
  });

  it('should show CA provisioning step', async () => {
    asMock(useConfigurationStep).mockReturnValue({ step: CONFIGURATION_STEPS.CERTIFICATE_PROVISIONING.key, isLoading: false, errors: null });
    renderPreflight(<ConfigurationWizard setIsWaitingForStartup={() => {}} />);

    await screen.findByText(/Certificate authority has been configured successfully./);
  });

  it('should show success step', async () => {
    asMock(useConfigurationStep).mockReturnValue({ step: CONFIGURATION_STEPS.CONFIGURATION_FINISHED.key, isLoading: false, errors: null });
    renderPreflight(<ConfigurationWizard setIsWaitingForStartup={() => {}} />);

    await screen.findByText(/The provisioning has been successful and all data nodes are secured and reachable./);
  });
});

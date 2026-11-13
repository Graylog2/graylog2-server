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
import { waitFor } from 'wrappedTestingLibrary';

import { SystemClusterConfig } from '@graylog/server-api';

import { asMock } from 'helpers/mocking';
import MockStore from 'helpers/mocking/StoreMock';
import { DEFAULT_PASSWORD_COMPLEXITY_CONFIG } from 'logic/users/passwordComplexity';

import usePasswordComplexityConfig from './usePasswordComplexityConfig';

jest.mock('@graylog/server-api', () => ({
  SystemClusterConfig: {
    read: jest.fn(),
  },
}));

// Outdated plugins can register an older copy of these actions, which lacks `listPasswordComplexityConfig`.
jest.mock('stores/configurations/ConfigurationsStore', () => ({
  ConfigurationsStore: MockStore(['getInitialState', () => ({ configuration: {} })]),
  ConfigurationsActions: {},
}));

const config = {
  min_length: 12,
  require_uppercase: true,
  require_lowercase: false,
  require_numbers: true,
  require_special_chars: false,
};

describe('usePasswordComplexityConfig', () => {
  it('returns the stored configuration', async () => {
    asMock(SystemClusterConfig.read).mockResolvedValue(config);

    const { result } = renderHook(() => usePasswordComplexityConfig());

    await waitFor(() => expect(result.current).toEqual(config));

    expect(SystemClusterConfig.read).toHaveBeenCalledWith('org.graylog2.users.PasswordComplexityConfig');
  });

  it('returns the default configuration when none is stored', async () => {
    asMock(SystemClusterConfig.read).mockResolvedValue(null);

    const { result } = renderHook(() => usePasswordComplexityConfig());

    await waitFor(() => expect(SystemClusterConfig.read).toHaveBeenCalled());

    expect(result.current).toEqual(DEFAULT_PASSWORD_COMPLEXITY_CONFIG);
  });
});
